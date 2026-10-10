package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** What AutoMod tells the user about their own message, as Twitch shows it. */
enum class AutoModNotice {
    Checking,
    Allowed,
    Removed,
}

/**
 * The rows for the user's own held message: on hold, their message and the AutoMod notice under it, since IRC does
 * not deliver a held message; on update, the notice of the decision. An allowed message then arrives through IRC.
 */
internal fun ownMessageHoldRows(raw: String, knownColors: Map<String, Color>, nowMillis: Long): List<ChatMessage> {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
    val payload = root.optJSONObject("payload") ?: return emptyList()
    val type = payload.optJSONObject("subscription")?.optString("type").orEmpty()
    val event = payload.optJSONObject("event") ?: return emptyList()
    val messageId = event.eventSubText("message_id") ?: return emptyList()
    return when (type) {
        "channel.chat.user_message_hold" -> {
            val message = event.optJSONObject("message") ?: return emptyList()
            listOfNotNull(
                eventSubChatRow(
                    id = "own-held-$messageId",
                    event = event,
                    message = message,
                    kind = ChatEventKind.Normal,
                    knownColors = knownColors,
                    timestampMillis = nowMillis,
                ),
                autoModNoticeRow(messageId, AutoModNotice.Checking, nowMillis),
            )
        }
        // Twitch marks a message nobody decided on in time `invalid`. It comes minutes later, when a notice no longer
        // says which message it means, so the user gets none, as on Twitch.
        "channel.chat.user_message_update" -> {
            val notice = when (event.optString("status").lowercase()) {
                "approved" -> AutoModNotice.Allowed
                "denied" -> AutoModNotice.Removed
                else -> return emptyList()
            }
            listOf(autoModNoticeRow(messageId, notice, nowMillis))
        }
        else -> emptyList()
    }
}

private fun autoModNoticeRow(messageId: String, notice: AutoModNotice, nowMillis: Long) = ChatMessage(
    id = "automod-notice-$messageId-${notice.name}",
    userLogin = "",
    displayName = "",
    color = Color.Unspecified,
    rawText = "",
    parts = emptyList(),
    timestampMillis = nowMillis,
    eventKind = ChatEventKind.System,
    autoModNotice = notice,
)
