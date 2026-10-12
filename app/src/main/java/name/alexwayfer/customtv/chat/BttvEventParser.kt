package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

internal object BttvEventParser {
    fun parseFrame(text: String): ChatMessage? {
        val json = text.toJsonObjectOrNull() ?: return null
        val data = json.optJSONObject("data") ?: return null
        return when (json.optString("name")) {
            "emote_create" -> parseCreateOrUpdate(data, EmoteChangeAction.Added)
            "emote_update" -> parseCreateOrUpdate(data, EmoteChangeAction.Renamed)
            "emote_delete" -> parseDelete(data)
            else -> null
        }
    }

    private fun parseCreateOrUpdate(data: JSONObject, action: EmoteChangeAction): ChatMessage? {
        val emoteJson = data.optJSONObject("emote") ?: return null
        val name = emoteJson.text("code") ?: return null
        val emoteId = emoteJson.text("id")
        val user = emoteJson.optJSONObject("user")
        val actorName = user?.text("displayName") ?: user?.text("name") ?: "Unknown"
        val emote = emoteId?.let { id ->
            SevenTvEmote(
                url = "$CDN/$id/2x.webp",
                aspectRatio = aspectRatio(emoteJson),
                id = id,
            )
        }
        return emoteChangeMessage(
            ChatEmoteChange(
                platform = EmotePlatform.Bttv,
                action = action,
                actorName = actorName,
                emoteName = name,
                emote = emote,
                emoteId = emoteId,
            ),
        )
    }

    private fun parseDelete(data: JSONObject): ChatMessage? {
        val emoteId = data.text("emoteId")
            ?: data.optJSONObject("emote")?.text("id")
            ?: return null
        val user = data.optJSONObject("user")
            ?: data.optJSONObject("emote")?.optJSONObject("user")
        val actorName = user?.text("displayName") ?: user?.text("name") ?: "Unknown"
        val name = data.optJSONObject("emote")?.text("code").orEmpty()
        val emote = SevenTvEmote(
            url = "$CDN/$emoteId/2x.webp",
            id = emoteId,
        )
        return emoteChangeMessage(
            ChatEmoteChange(
                platform = EmotePlatform.Bttv,
                action = EmoteChangeAction.Removed,
                actorName = actorName,
                emoteName = name,
                emote = emote,
                emoteId = emoteId,
            ),
        )
    }

    private fun aspectRatio(item: JSONObject): Float {
        val width = item.optInt("width")
        val height = item.optInt("height")
        if (width <= 0 || height <= 0) return 1f
        return width.toFloat() / height.toFloat()
    }

    private const val CDN = "https://cdn.betterttv.net/emote"
}
