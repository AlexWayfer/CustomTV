package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.toJsonObjectOrNull
import org.json.JSONObject

/**
 * The rows for the user's own held message: on hold, their message and the AutoMod notice under it, since IRC does
 * not deliver a held message; on update, the notice of the decision. An allowed message then arrives through IRC.
 */
internal fun ownMessageHoldRows(raw: String, knownColors: Map<String, Color>, nowMillis: Long): List<ChatMessage> {
    val root = raw.toJsonObjectOrNull() ?: return emptyList()
    val payload = root.optJSONObject("payload") ?: return emptyList()
    val event = payload.optJSONObject("event") ?: return emptyList()
    val messageId = event.eventSubText("message_id") ?: return emptyList()
    return when (payload.optJSONObject("subscription")?.optString("type").orEmpty()) {
        "channel.chat.user_message_hold" -> heldMessageRows(event, messageId, knownColors, nowMillis)
        "channel.chat.user_message_update" -> decisionRows(event, messageId, nowMillis)
        else -> emptyList()
    }
}

private fun heldMessageRows(
    event: JSONObject,
    messageId: String,
    knownColors: Map<String, Color>,
    nowMillis: Long,
): List<ChatMessage> {
    if (event.optJSONObject("message") == null) return emptyList()
    return listOfNotNull(
        eventSubChatRow(
            id = "own-held-$messageId",
            event = event,
            kind = ChatEventKind.Normal,
            knownColors = knownColors,
            timestampMillis = nowMillis,
        ),
        autoModNoticeRow(messageId, AutoModNotice.Checking, nowMillis),
    )
}

// Twitch marks a message nobody decided on in time `invalid`. It comes minutes later, when a notice no longer
// says which message it means, so the user gets none, as on Twitch.
private fun decisionRows(event: JSONObject, messageId: String, nowMillis: Long): List<ChatMessage> {
    val notice = when (event.optString("status").lowercase()) {
        "approved" -> AutoModNotice.Allowed
        "denied" -> AutoModNotice.Removed
        else -> null
    }
    return listOfNotNull(notice?.let { autoModNoticeRow(messageId, it, nowMillis) })
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
