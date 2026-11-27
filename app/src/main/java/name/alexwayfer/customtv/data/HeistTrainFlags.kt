package name.alexwayfer.customtv.data

import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import name.alexwayfer.customtv.chat.HeistTrainFlags
import okhttp3.Request
import org.json.JSONObject

private const val THEME = "Hype-Train-GTA-VI-Theme"
private const val SKIP_CATEGORY = "Hype-Train-GTA-VI-Skip-Category-Check"

/** The same public configuration that twitch.tv preloads, requested as JSON rather than JSONP. */
internal object TwitchHeistTrainFlags {
    private const val PUBLIC_WEB_KEY = "3mv-BKCSZJDyZrwfqK86har6u13NgxRh.ZWg9bTRsZHJnLmUuZXBwby5jbG91ZCZjcz1tNGxkcmc"
    private const val CONFIG_URL = "https://assets.twitch.tv/eppo/api/flag-config/v1/config"
    private val request = Request.Builder()
        .url("$CONFIG_URL?sdkName=js-sdk-client&sdkVersion=3.1.2&apiKey=$PUBLIC_WEB_KEY")
        .build()
    private val client = sharedHttpClient.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
    private var cached: JSONObject? = null
    private var freshUntil = 0L
    private val deviceId = UUID.randomUUID().toString()

    @Synchronized
    fun get(viewerId: String?): HeistTrainFlags? {
        val now = System.currentTimeMillis()
        val flags = cached?.takeIf { now < freshUntil } ?: fetch() ?: return null
        return heistTrainFlags(flags, now, HeistTrainViewer(deviceId, viewerId))
    }

    private fun fetch(): JSONObject? = try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val flags = response.body.string().toJsonObjectOrNull()?.optJSONObject("flags") ?: return null
            val maxAge = response.cacheControl.maxAgeSeconds.toLong() - (response.header("Age")?.toLongOrNull() ?: 0L)
            freshUntil = response.receivedResponseAtMillis + TimeUnit.SECONDS.toMillis(maxAge.coerceAtLeast(0L))
            JSONObject().put(THEME, flags.optJSONObject(THEME)).put(SKIP_CATEGORY, flags.optJSONObject(SKIP_CATEGORY))
                .also { cached = it }
        }
    } catch (_: IOException) {
        null
    }
}

internal data class HeistTrainViewer(val deviceId: String, val userId: String?)

internal fun heistTrainFlags(flags: JSONObject, now: Long, viewer: HeistTrainViewer): HeistTrainFlags =
    HeistTrainFlags(flags.assignment(THEME, now, viewer), flags.assignment(SKIP_CATEGORY, now, viewer))

private const val USER_ENTITY = 143
private const val DEVICE_ENTITY = 144

/** The campaign's Eppo rules use viewer or device allowlists followed by scheduled traffic splits. */
private fun JSONObject.assignment(key: String, now: Long, viewer: HeistTrainViewer): Boolean {
    val flag = optJSONObject(key) ?: return false
    if (!flag.optBoolean("enabled")) return false
    val subject = when (flag.optInt("entityId")) {
        USER_ENTITY -> viewer.userId
        DEVICE_ENTITY -> viewer.deviceId
        else -> null
    } ?: return false
    val total = flag.optInt("totalShards")
    val split = flag.optJSONArray("allocations")?.objects().orEmpty().asSequence()
        .filter { it.activeAt(now) && it.matchesViewer(viewer, subject) }
        .flatMap { it.optJSONArray("splits")?.objects().orEmpty() }
        .firstOrNull { split ->
            split.optJSONArray("shards")?.objects()?.all { shard -> shard.containsSubject(subject, total) } == true
        }
    val variation = split?.optString("variationKey") ?: return false
    return flag.optJSONObject("variations")?.optJSONObject(variation)?.opt("value") == true
}

private fun JSONObject.activeAt(now: Long): Boolean =
    now >= (trimmedText("startAt")?.let(::instantMillisOrNull) ?: Long.MIN_VALUE) &&
        now < (trimmedText("endAt")?.let(::instantMillisOrNull) ?: Long.MAX_VALUE)

private fun JSONObject.matchesViewer(viewer: HeistTrainViewer, subject: String): Boolean {
    val rules = optJSONArray("rules")?.objects().orEmpty()
    return rules.isEmpty() || rules.any { rule ->
        rule.optJSONArray("conditions")?.objects()?.all { condition ->
            val attribute = when (condition.optString("attribute")) {
                "user_id" -> viewer.userId
                "device_id" -> viewer.deviceId
                "id" -> subject
                else -> null
            }
            val values = condition.optJSONArray("value")
            condition.optString("operator") == "ONE_OF" && values != null &&
                (0 until values.length()).any { values.optString(it) == attribute }
        } == true
    }
}

private fun JSONObject.containsSubject(subject: String, total: Int): Boolean {
    if (total <= 0) return false
    val salt = optString("salt")
    val digest = MessageDigest.getInstance("MD5").digest("$salt-$subject".toByteArray(Charsets.UTF_8))
    val shard = (ByteBuffer.wrap(digest).int.toUInt().toLong() % total).toInt()
    return optJSONArray("ranges")?.objects().orEmpty().any { shard in it.optInt("start") until it.optInt("end") }
}
