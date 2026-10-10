package name.alexwayfer.customtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CheermoteTier(val minBits: Int, val imageUrl: String)

object CheermoteRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val cache = mutableMapOf<String, Map<String, List<CheermoteTier>>>()

    @Synchronized
    fun cached(channel: String): Map<String, List<CheermoteTier>>? = cache[channel.lowercase()]

    suspend fun forChannel(channel: String): Map<String, List<CheermoteTier>> = withContext(Dispatchers.IO) {
        val key = channel.lowercase()
        cached(key)?.let { return@withContext it }
        val body = JSONObject()
            .put("query", $$"query($login:String!){user(login:$login){id cheer{emotes{prefix tiers{bits images{theme isAnimated dpiScale url}}}}}}")
            .put("variables", JSONObject().put("login", key))
            .toString()
        val request = twitchGqlRequest(body).build()
        val result = http.newCall(request).execute().use { response ->
            val text = response.body.string()
            Diagnostics.reportGql("Cheermotes", "cheermotes", response.code, text)
            if (!response.isSuccessful) return@use emptyMap()
            val json = JSONObject(text)
            json.optJSONObject("data")?.optJSONObject("user")?.optString("id")
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { userId -> AssetFiles.write(AssetFiles.channel(userId, FILE), text) }
            parse(json)
        }
        if (result.isNotEmpty()) synchronized(this@CheermoteRepository) { cache[key] = result }
        result
    }

    /** The cheer images the last start kept for [channel], shown until [forChannel] answers. */
    suspend fun restore(channel: String): Map<String, List<CheermoteTier>>? = withContext(Dispatchers.IO) {
        cached(channel)?.let { return@withContext it }
        ChannelAvatarRepository.awaitRestored()
        val userId = ChannelAvatarRepository.cached(channel)?.id ?: return@withContext null
        EmoteHttp.stored(AssetFiles.channel(userId, FILE)) { parse(JSONObject(it)) }?.takeIf { it.isNotEmpty() }
    }

    private const val FILE = "cheermotes"

    internal fun parse(json: JSONObject): Map<String, List<CheermoteTier>> {
        val emotes = json.optJSONObject("data")?.optJSONObject("user")
            ?.optJSONObject("cheer")?.optJSONArray("emotes") ?: return emptyMap()
        val result = linkedMapOf<String, List<CheermoteTier>>()
        for (index in 0 until emotes.length()) {
            val item = emotes.optJSONObject(index) ?: continue
            val prefix = item.optString("prefix").takeIf { it.isNotBlank() } ?: continue
            val tiers = item.optJSONArray("tiers") ?: continue
            val mapped = mutableListOf<CheermoteTier>()
            for (tierIndex in 0 until tiers.length()) {
                val tier = tiers.optJSONObject(tierIndex) ?: continue
                val bits = tier.optInt("bits")
                if (bits <= 0) continue
                val images = tier.optJSONArray("images") ?: continue
                var url: String? = null
                for (imageIndex in 0 until images.length()) {
                    val image = images.optJSONObject(imageIndex) ?: continue
                    if (image.optString("theme") == "DARK" && image.optBoolean("isAnimated") &&
                        image.optDouble("dpiScale") == 2.0
                    ) {
                        url = image.optString("url").takeIf { it.startsWith("https://") }
                        break
                    }
                }
                if (url != null) mapped += CheermoteTier(bits, url)
            }
            if (mapped.isNotEmpty()) result[prefix.lowercase()] = mapped.sortedBy { it.minBits }
        }
        return result
    }
}
