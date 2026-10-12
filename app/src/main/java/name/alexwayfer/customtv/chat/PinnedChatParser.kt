package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.text
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

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

sealed interface PinnedChatUpdate {
    data class Set(val pin: PinnedChat) : PinnedChatUpdate
    data class Clear(val pinId: String? = null) : PinnedChatUpdate
    data class Duration(val pinId: String?, val endsAtMillis: Long?) : PinnedChatUpdate
    data object Refresh : PinnedChatUpdate
}

object PinnedChatParser {
    fun parseFrame(raw: String): PinnedChatUpdate? {
        val frame = raw.toJsonObjectOrNull() ?: return null
        if (frame.optString("type") != "MESSAGE") return null
        val data = frame.optJSONObject("data") ?: return null
        val topic = data.optString("topic")
        if (topic.isNotEmpty() && !topic.startsWith(TOPIC_PREFIX)) return null
        val inner = data.text("message") ?: return null
        return parseUpdate(inner)
    }

    fun parseUpdate(raw: String): PinnedChatUpdate? {
        val payload = raw.toJsonObjectOrNull() ?: return null
        return when (payload.optString("type")) {
            "pin-message" -> parsePin(payload.pinObject("data") ?: payload)?.let { PinnedChatUpdate.Set(it) }
                ?: PinnedChatUpdate.Refresh
            "unpin-message" -> PinnedChatUpdate.Clear(pinId(payload))
            "update-message", "update-pin" -> parseDurationOrRefresh(payload)
            else -> null
        }
    }

    fun parseGqlBody(raw: String): PinnedChat? {
        val data = raw.toJsonObjectOrNull()?.optJSONObject("data")
            ?: return null
        val edges = data.optJSONObject("channel")?.optJSONObject("pinnedChatMessages")
            ?.optJSONArray("edges")
            ?: return null
        if (edges.length() == 0) return null
        val node = edges.optJSONObject(0)?.optJSONObject("node") ?: return null
        return parsePin(node)
    }

    fun parsePin(node: JSONObject): PinnedChat? {
        val pinId = node.pinText("id") ?: return null
        val messageObj = node.pinObject("pinnedMessage", "pinned_message", "message") ?: return null
        val message = PinnedMessageParser.parse(messageObj) ?: return null
        return PinnedChat(
            pinId = pinId,
            message = message,
            pinnedBy = parsePinnedBy(node),
            endsAtMillis = pinTimeMillis(node.pinText("endsAt", "ends_at")),
        )
    }

    private fun parseDurationOrRefresh(payload: JSONObject): PinnedChatUpdate {
        val data = payload.pinObject("data") ?: payload
        parsePin(data)?.let { return PinnedChatUpdate.Set(it) }
        val pinId = pinId(payload)
        val endsAt = pinTimeMillis(data.pinText("endsAt", "ends_at"))
        return if (pinId != null || endsAt != null) {
            PinnedChatUpdate.Duration(pinId, endsAt)
        } else {
            PinnedChatUpdate.Refresh
        }
    }

    private fun pinId(payload: JSONObject): String? {
        val data = payload.pinObject("data") ?: payload
        return data.pinText("id")
    }

    private fun parsePinnedBy(node: JSONObject): PinnedBy? {
        val obj = node.pinObject("pinnedBy", "pinned_by") ?: return null
        val displayName = obj.pinText("displayName", "display_name") ?: return null
        val login = obj.pinText("login")?.lowercase() ?: displayName.lowercase()
        return PinnedBy(
            login = login,
            displayName = displayName,
            badges = PinnedMessageParser.parseBadges(
                obj.optJSONArray("displayBadges") ?: obj.optJSONArray("display_badges"),
            ),
        )
    }

    private const val TOPIC_PREFIX = "pinned-chat-updates-v1."
}
