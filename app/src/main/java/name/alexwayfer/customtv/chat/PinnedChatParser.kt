package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.ui.theme.parseTwitchHexColor
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class PinnedBy(
    val login: String,
    val displayName: String,
    val badges: List<ChatBadge> = emptyList(),
) {
    fun pinRoleBadge(channelLogin: String): ChatBadge {
        val isBroadcaster = login.equals(channelLogin, ignoreCase = true) ||
            badges.any { it.setId.equals("broadcaster", ignoreCase = true) }
        if (isBroadcaster) {
            return badges.firstOrNull { it.setId.equals("broadcaster", ignoreCase = true) }
                ?: ChatBadge("broadcaster", "1")
        }
        return badges.firstOrNull { it.setId.equals("moderator", ignoreCase = true) }
            ?: ChatBadge("moderator", "1")
    }
}

data class PinnedChat(
    val pinId: String,
    val message: ChatMessage,
    val pinnedBy: PinnedBy? = null,
    val endsAtMillis: Long? = null,
    /** Pinned while chat was open, rather than already pinned when it loaded. */
    val live: Boolean = false,
)

sealed class PinnedChatUpdate {
    data class Set(val pin: PinnedChat) : PinnedChatUpdate()
    data class Clear(val pinId: String? = null) : PinnedChatUpdate()
    data class Duration(val pinId: String?, val endsAtMillis: Long?) : PinnedChatUpdate()
    data object Refresh : PinnedChatUpdate()
}

object PinnedChatParser {
    fun parseFrame(raw: String): PinnedChatUpdate? {
        val frame = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (frame.optString("type") != "MESSAGE") return null
        val data = frame.optJSONObject("data") ?: return null
        val topic = data.optString("topic")
        if (topic.isNotEmpty() && !topic.startsWith(TOPIC_PREFIX)) return null
        val inner = data.optString("message").takeIf { it.isNotBlank() && it != "null" }
            ?: return null
        return parseUpdate(inner)
    }

    fun parseUpdate(raw: String): PinnedChatUpdate? {
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return when (payload.optString("type")) {
            "pin-message" -> parsePin(payload.obj("data") ?: payload)?.let { PinnedChatUpdate.Set(it) }
                ?: PinnedChatUpdate.Refresh
            "unpin-message" -> PinnedChatUpdate.Clear(pinId(payload))
            "update-message", "update-pin" -> parseDurationOrRefresh(payload)
            else -> null
        }
    }

    fun parseGqlBody(raw: String): PinnedChat? {
        val data = runCatching { JSONObject(raw) }.getOrNull()?.optJSONObject("data")
            ?: return null
        val edges = data.optJSONObject("channel")?.optJSONObject("pinnedChatMessages")
            ?.optJSONArray("edges")
            ?: return null
        if (edges.length() == 0) return null
        val node = edges.optJSONObject(0)?.optJSONObject("node") ?: return null
        return parsePin(node)
    }

    fun parsePin(node: JSONObject): PinnedChat? {
        val pinId = node.str("id") ?: return null
        val messageObj = node.obj("pinnedMessage", "pinned_message", "message") ?: return null
        val message = parseChatMessage(messageObj) ?: return null
        return PinnedChat(
            pinId = pinId,
            message = message,
            pinnedBy = parsePinnedBy(node),
            endsAtMillis = timestampMillis(node.str("endsAt", "ends_at")),
        )
    }

    private fun parseDurationOrRefresh(payload: JSONObject): PinnedChatUpdate {
        val data = payload.obj("data") ?: payload
        parsePin(data)?.let { return PinnedChatUpdate.Set(it) }
        val pinId = pinId(payload)
        val endsAt = timestampMillis(data.str("endsAt", "ends_at"))
        return if (pinId != null || endsAt != null) {
            PinnedChatUpdate.Duration(pinId, endsAt)
        } else {
            PinnedChatUpdate.Refresh
        }
    }

    private fun pinId(payload: JSONObject): String? {
        val data = payload.obj("data") ?: payload
        return data.str("id")
    }

    private fun parsePinnedBy(node: JSONObject): PinnedBy? {
        val obj = node.obj("pinnedBy", "pinned_by") ?: return null
        val displayName = obj.str("displayName", "display_name") ?: return null
        val login = obj.str("login")?.lowercase() ?: displayName.lowercase()
        return PinnedBy(
            login = login,
            displayName = displayName,
            badges = parseBadges(
                obj.optJSONArray("displayBadges") ?: obj.optJSONArray("display_badges"),
            ),
        )
    }

    private fun parseChatMessage(obj: JSONObject): ChatMessage? {
        val sender = obj.obj("sender") ?: return null
        val content = obj.obj("content")
        val text = content?.str("text") ?: obj.str("text").orEmpty()
        val displayName = sender.str("displayName", "display_name") ?: return null
        val login = sender.str("login")?.lowercase()
            ?: displayName.lowercase()
        val color = parseTwitchHexColor(sender.str("chatColor", "chat_color"))
            ?: IrcMessageParser.nameColor(displayName.ifEmpty { login })
        val reply = parseReply(obj)
        var displayText = text
        if (reply != null) {
            displayText = IrcMessageParser.stripLeadingReplyMention(displayText, reply)
        }
        val parts = parseFragments(content?.optJSONArray("fragments"), displayText)
            .ifEmpty { if (displayText.isEmpty()) emptyList() else listOf(ChatPart.Text(displayText)) }
        val id = obj.str("id") ?: "pin-$login-${System.nanoTime()}"
        val timestampMillis = timestampMillis(
            obj.str("sentAt", "sent_at"),
        ) ?: System.currentTimeMillis()
        return ChatMessage(
            id = id,
            userId = sender.str("id"),
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
        val parent = obj.obj("parentMessage", "parent_message") ?: return null
        val parentId = parent.str("id") ?: return null
        val parentSender = parent.obj("sender")
        val parentDisplay = parentSender?.str("displayName", "display_name").orEmpty()
        val parentLogin = parentSender?.str("login")?.lowercase().orEmpty()
        if (parentDisplay.isBlank() && parentLogin.isBlank()) return null
        val parentBody = parent.obj("content")?.str("text")
            ?: parent.str("text").orEmpty()
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
            val text = fragment.str("text").orEmpty()
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

    private fun emoteId(fragment: JSONObject): String? {
        fragment.obj("content")?.str("emoteID", "emoteId")?.let { return it }
        fragment.obj("emoticon")?.str("emoticonID", "emoticonId", "id")?.let { return it }
        return fragment.str("emoteID", "emoteId")
    }

    /** GQL sends `displayBadges` with `setID`; PubSub sends `badges` with the set in `id`. */
    private fun parseSenderBadges(sender: JSONObject): List<ChatBadge> {
        val display = sender.optJSONArray("displayBadges") ?: sender.optJSONArray("display_badges")
        if (display != null) return parseBadges(display)
        return parseBadges(sender.optJSONArray("badges"), "setID", "setId", "set_id", "id")
    }

    private fun parseBadges(
        array: JSONArray?,
        vararg setIdKeys: String = arrayOf("setID", "setId", "set_id"),
    ): List<ChatBadge> {
        if (array == null) return emptyList()
        val badges = ArrayList<ChatBadge>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val setId = item.str(*setIdKeys) ?: continue
            val version = item.str("version") ?: continue
            badges += ChatBadge(setId, version)
        }
        return badges
    }

    private fun timestampMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }

    private fun JSONObject.obj(vararg keys: String): JSONObject? {
        for (key in keys) {
            if (has(key) && !isNull(key)) optJSONObject(key)?.let { return it }
        }
        return null
    }

    private fun JSONObject.str(vararg keys: String): String? {
        for (key in keys) {
            optString(key).takeIf { it.isNotBlank() && it != "null" }?.let { return it }
        }
        return null
    }

    private const val TOPIC_PREFIX = "pinned-chat-updates-v1."
}
