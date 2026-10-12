package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.objects
import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor
import org.json.JSONArray
import org.json.JSONObject

/** The pinned message itself, as GQL and PubSub send it: its text, reply, emotes, and badges. */
internal object PinnedMessageParser {
    fun parse(obj: JSONObject): ChatMessage? {
        val sender = obj.pinObject("sender") ?: return null
        val content = obj.pinObject("content")
        val text = content?.pinText("text") ?: obj.pinText("text").orEmpty()
        val displayName = sender.pinText("displayName", "display_name") ?: return null
        val login = sender.pinText("login")?.lowercase()
            ?: displayName.lowercase()
        val color = parseTwitchHexColor(sender.pinText("chatColor", "chat_color"))
            ?: IrcMessageParser.nameColor(displayName.ifEmpty { login })
        val reply = parseReply(obj)
        var displayText = text
        if (reply != null) {
            displayText = IrcMessageText.stripLeadingReplyMention(displayText, reply)
        }
        val parts = parseFragments(content?.optJSONArray("fragments"), displayText)
            .ifEmpty { if (displayText.isEmpty()) emptyList() else listOf(ChatPart.Text(displayText)) }
        val id = obj.pinText("id") ?: "pin-$login-${System.nanoTime()}"
        val timestampMillis = pinTimeMillis(
            obj.pinText("sentAt", "sent_at"),
        ) ?: System.currentTimeMillis()
        return ChatMessage(
            id = id,
            userId = sender.pinText("id"),
            userLogin = login,
            displayName = displayName,
            color = color,
            rawText = displayText,
            parts = parts,
            timestampMillis = timestampMillis,
            badges = parseSenderBadges(sender),
            reply = reply,
        )
    }

    private fun parseReply(obj: JSONObject): ChatReply? {
        val parent = obj.pinObject("parentMessage", "parent_message") ?: return null
        val parentId = parent.pinText("id") ?: return null
        val parentSender = parent.pinObject("sender")
        val parentDisplay = parentSender?.pinText("displayName", "display_name").orEmpty()
        val parentLogin = parentSender?.pinText("login")?.lowercase().orEmpty()
        if (parentDisplay.isBlank() && parentLogin.isBlank()) return null
        val parentBody = parent.pinObject("content")?.pinText("text")
            ?: parent.pinText("text").orEmpty()
        return ChatReply(
            parentMsgId = parentId,
            parentUserLogin = parentLogin.ifEmpty { parentDisplay.lowercase() },
            parentDisplayName = parentDisplay.ifEmpty { parentLogin },
            parentBody = parentBody,
        )
    }

    private fun parseFragments(fragments: JSONArray?, fallbackText: String): List<ChatPart> {
        if (fragments == null || fragments.length() == 0) {
            return if (fallbackText.isEmpty()) emptyList() else listOf(ChatPart.Text(fallbackText))
        }
        val parts = ArrayList<ChatPart>(fragments.length())
        for (index in 0 until fragments.length()) {
            val fragment = fragments.optJSONObject(index) ?: continue
            val text = fragment.pinText("text").orEmpty()
            val emoteId = emoteId(fragment)
            if (emoteId != null) {
                parts += ChatPart.Emote(
                    name = text.ifEmpty { emoteId },
                    url = twitchEmoteUrl(emoteId),
                )
            } else if (text.isNotEmpty()) {
                parts += ChatPart.Text(text)
            }
        }
        return parts
    }

    private fun emoteId(fragment: JSONObject): String? =
        fragment.pinObject("content")?.pinText("emoteID", "emoteId")
            ?: fragment.pinObject("emoticon")?.pinText("emoticonID", "emoticonId", "id")
            ?: fragment.pinText("emoteID", "emoteId")

    /** GQL sends `displayBadges` with `setID`; PubSub sends `badges` with the set in `id`. */
    private fun parseSenderBadges(sender: JSONObject): List<ChatBadge> {
        val display = sender.optJSONArray("displayBadges") ?: sender.optJSONArray("display_badges")
        if (display != null) return parseBadges(display)
        return parseBadges(sender.optJSONArray("badges"), "setID", "setId", "set_id", "id")
    }

    fun parseBadges(
        array: JSONArray?,
        vararg setIdKeys: String = arrayOf("setID", "setId", "set_id"),
    ): List<ChatBadge> {
        if (array == null) return emptyList()
        return array.objects().mapNotNull { item ->
            val setId = item.pinText(*setIdKeys)
            val version = item.pinText("version")
            if (setId != null && version != null) ChatBadge(setId, version) else null
        }
    }
}
