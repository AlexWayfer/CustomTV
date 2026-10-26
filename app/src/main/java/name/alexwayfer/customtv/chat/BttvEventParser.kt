package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

internal object BttvEventParser {
    fun parseFrame(text: String): BttvFrame? {
        val json = text.toJsonObjectOrNull() ?: return null
        val data = json.optJSONObject("data") ?: return null
        val message = when (json.optString("name")) {
            "emote_create" -> parseCreateOrUpdate(data, EmoteChangeAction.Added)
            "emote_update" -> parseCreateOrUpdate(data, EmoteChangeAction.Renamed)
            "emote_delete" -> parseDelete(data)
            "lookup_user" -> return parseLookupUser(data)?.let(BttvFrame::Personal)
            else -> null
        }
        return message?.let(BttvFrame::EmoteChange)
    }

    /** A chatter that told BTTV it is in the channel; only a Pro one has personal emotes. */
    private fun parseLookupUser(data: JSONObject): PersonalEmoteEvent.BttvUser? {
        if (!data.optBoolean("pro")) return null
        val userId = data.text("providerId") ?: return null
        val emotes = data.optJSONArray("emotes")?.objects().orEmpty().mapNotNull { item ->
            val code = item.text("code") ?: return@mapNotNull null
            val id = item.text("id") ?: return@mapNotNull null
            code to SevenTvEmote(url = "$CDN/$id/2x.webp", aspectRatio = aspectRatio(item), id = id)
        }
        return PersonalEmoteEvent.BttvUser(userId, emotes.toMap())
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

internal sealed interface BttvFrame {
    data class EmoteChange(val message: ChatMessage) : BttvFrame
    data class Personal(val event: PersonalEmoteEvent.BttvUser) : BttvFrame
}
