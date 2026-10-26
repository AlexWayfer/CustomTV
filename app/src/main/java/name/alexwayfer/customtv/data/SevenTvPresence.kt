package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.json.JSONObject
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/**
 * Tells 7TV that the user is in a Twitch channel, as its extension does. 7TV then sends the user's personal sets,
 * badge, and paint to every chat open there, this app's own among them.
 */
internal object SevenTvPresence {
    suspend fun send(sevenTvUserId: String, channelId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://7tv.io/v3/users/$sevenTvUserId/presences")
            .post(sevenTvPresenceBody(channelId).toString().toRequestBody(JSON))
            .build()
        try {
            sharedHttpClient.newCall(request).executeAsync().use { response ->
                if (response.isSuccessful) return@use
                AppLog.w(TAG, "presence failed HTTP ${response.code}")
                Diagnostics.reportHttp(TAG, "7tv users/presences", response.code, "", emptySet())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            AppLog.w(TAG, "presence failed ${error.javaClass.simpleName}")
        }
    }

    private const val TAG = "SevenTvPresence"
    private val JSON = "application/json".toMediaType()
}

/** A channel presence; 7TV reads no more than its kind and the channel. */
internal fun sevenTvPresenceBody(channelId: String): JSONObject = JSONObject()
    .put("kind", PRESENCE_KIND_CHANNEL)
    .put("data", JSONObject().put("platform", "TWITCH").put("id", channelId))

/** Whether the user's message should announce them again: once in [SEVEN_TV_PRESENCE_GAP], as Xtra does. */
internal fun sevenTvPresenceDue(lastSentAtMillis: Long?, nowMillis: Long): Boolean =
    lastSentAtMillis == null || nowMillis - lastSentAtMillis >= SEVEN_TV_PRESENCE_GAP.inWholeMilliseconds

private const val PRESENCE_KIND_CHANNEL = 1
private val SEVEN_TV_PRESENCE_GAP = 10.seconds
