package name.alexwayfer.customtv.chat

import org.json.JSONObject

internal object BttvEventParser {
    fun parseFrame(text: String): ChatMessage? {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
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
        val name = emoteJson.optString("code").takeIf { it.isNotBlank() && it != "null" } ?: return null
        val emoteId = emoteJson.optString("id").takeIf { it.isNotBlank() && it != "null" }
        val user = emoteJson.optJSONObject("user")
        val actorName = user?.optString("displayName")?.takeIf { it.isNotBlank() && it != "null" }
            ?: user?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }
            ?: "Unknown"
        val emote = emoteId?.let { id ->
            SevenTvEmote(
                url = "$CDN/$id/2x.webp",
                aspectRatio = aspectRatio(emoteJson),
                id = id,
            )
        }
        return emoteChangeMessage(
            platform = EmotePlatform.Bttv,
            action = action,
            actorName = actorName,
            emoteName = name,
            emote = emote,
            emoteId = emoteId,
        )
    }

    private fun parseDelete(data: JSONObject): ChatMessage? {
        val emoteId = data.optString("emoteId").takeIf { it.isNotBlank() && it != "null" }
            ?: data.optJSONObject("emote")?.optString("id")?.takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        val user = data.optJSONObject("user")
            ?: data.optJSONObject("emote")?.optJSONObject("user")
        val actorName = user?.optString("displayName")?.takeIf { it.isNotBlank() && it != "null" }
            ?: user?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }
            ?: "Unknown"
        val name = data.optJSONObject("emote")
            ?.optString("code")
            ?.takeIf { it.isNotBlank() && it != "null" }
            .orEmpty()
        val emote = SevenTvEmote(
            url = "$CDN/$emoteId/2x.webp",
            id = emoteId,
        )
        return emoteChangeMessage(
            platform = EmotePlatform.Bttv,
            action = EmoteChangeAction.Removed,
            actorName = actorName,
            emoteName = name,
            emote = emote,
            emoteId = emoteId,
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
