package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.SevenTvEmote
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object SevenTvRepository {
    private val channelSetIds = ConcurrentHashMap<String, String>()
    internal val emotes = EmoteSourceCache(
        file = "7tv",
        globalUrl = "$API/emote-sets/global",
        channelUrl = { twitchUserId -> "$API/users/twitch/$twitchUserId" },
        parseGlobal = ::parseSet,
        parseChannel = { twitchUserId, body -> EmoteChannel(parseChannel(twitchUserId, body)) },
    )

    fun channelEmoteSetId(twitchUserId: String?): String? {
        if (twitchUserId.isNullOrBlank()) return null
        return channelSetIds[twitchUserId]
    }

    private fun parseChannel(twitchUserId: String, body: String): Map<String, SevenTvEmote> {
        val json = JSONObject(body)
        val set = json.optJSONObject("emote_set")
        val setId = set?.text("id") ?: json.text("emote_set_id")
        if (setId != null) {
            channelSetIds[twitchUserId] = setId
        }
        return parseEmotes(set?.optJSONArray("emotes"))
    }

    private fun parseSet(body: String): Map<String, SevenTvEmote> =
        parseEmotes(JSONObject(body).optJSONArray("emotes"))

    internal fun parseSetItem(item: JSONObject): Pair<String, SevenTvEmote>? {
        val name = item.text("name") ?: return null
        val host = item.optJSONObject("data")?.optJSONObject("host")
            ?: item.optJSONObject("host")
        val url = emoteUrl(host) ?: return null
        val id = item.optJSONObject("data")?.text("id") ?: item.text("id")
        val data = item.optJSONObject("data")
        val tags = data?.optJSONArray("tags")?.let { array ->
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
        }.orEmpty()
        val originalName = data?.text("name")?.takeIf { it != name }
        return name to SevenTvEmote(
            url = url,
            aspectRatio = aspectRatio(host),
            overlay = isOverlay(item),
            id = id,
            tags = tags,
            originalName = originalName,
        )
    }

    private fun parseEmotes(array: JSONArray?): Map<String, SevenTvEmote> {
        return array?.objects()?.mapNotNull(::parseSetItem)?.toMap().orEmpty()
    }

    private fun emoteUrl(host: JSONObject?): String? {
        val raw = host?.text("url") ?: return null
        val base = if (raw.startsWith("//")) "https:$raw" else raw.trimEnd('/')
        return "$base/2x.webp"
    }

    private fun aspectRatio(host: JSONObject?): Float {
        val files = host?.optJSONArray("files")?.objects()
            ?.filter { it.optInt("width") > 0 && it.optInt("height") > 0 }
            ?: return 1f
        // The 2x WebP the chat shows, or else the first WebP of any size.
        val file = files.firstOrNull { it.optString("name") == "2x.webp" }
            ?: files.firstOrNull { it.optString("format").equals("WEBP", ignoreCase = true) }
            ?: return 1f
        return file.optInt("width").toFloat() / file.optInt("height").toFloat()
    }

    private fun isOverlay(item: JSONObject): Boolean {
        val dataFlags = item.optJSONObject("data")?.optInt("flags") ?: 0
        return (dataFlags or item.optInt("flags")) and FLAG_ZERO_WIDTH != 0
    }

    private const val API = "https://7tv.io/v3"
    private const val FLAG_ZERO_WIDTH = 256
}
