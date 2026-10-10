package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/**
 * A chatter was timed out or banned. Anonymous IRC names only the chatter; a moderator's EventSub also names the
 * moderator. Both report the same action, so a moderator sees one row filled from both.
 */
data class ChatModerationNotice(
    val targetLogin: String,
    val targetName: String,
    /** Null for a ban. */
    val timeoutSeconds: Long?,
    val moderatorName: String? = null,
    val fromIrc: Boolean = false,
)

/** A moderator warned a chatter, or the chatter acknowledged a warning. Only the channel's moderators learn of either. */
data class ChatWarningNotice(
    val targetName: String,
    val acknowledged: Boolean,
    /** Who warned; empty on an acknowledgement. */
    val moderatorName: String = "",
    val reason: String = "",
    val citedRules: List<String> = emptyList(),
)

/** How far apart IRC and EventSub may report the same action and still fill one row. */
private val SAME_ACTION_WINDOW = 30.seconds

internal fun moderationNoticeRow(id: String, notice: ChatModerationNotice, timestampMillis: Long) = ChatMessage(
    id = id,
    userLogin = "",
    displayName = "",
    color = Color.Unspecified,
    rawText = "",
    parts = emptyList(),
    timestampMillis = timestampMillis,
    eventKind = ChatEventKind.System,
    moderationNotice = notice,
)

/** The row for an IRC `CLEARCHAT` that names a chatter, parsed as [ChatEventKind.UserMessagesDeleted]. */
internal fun clearChatNoticeRow(event: ChatMessage): ChatMessage = moderationNoticeRow(
    id = "${event.id}-notice",
    notice = ChatModerationNotice(
        targetLogin = event.userLogin,
        targetName = event.displayName.ifBlank { event.userLogin },
        timeoutSeconds = event.timeoutSeconds,
        fromIrc = true,
    ),
    timestampMillis = event.timestampMillis,
)

/**
 * Adds [row] as a new line, or fills the line the other source already added for the same action: IRC brings the
 * exact duration, EventSub the moderator and the chatter's display name. The oldest unfilled line pairs first.
 */
internal fun withModerationNotice(messages: List<ChatMessage>, row: ChatMessage): List<ChatMessage> {
    val incoming = row.moderationNotice ?: return messages
    val index = messages.indexOfFirst { message ->
        val shown = message.moderationNotice
        shown != null &&
            sameAction(shown, incoming) &&
            abs(message.timestampMillis - row.timestampMillis) <= SAME_ACTION_WINDOW.inWholeMilliseconds
    }
    val shown = messages.getOrNull(index)?.moderationNotice
        ?: return withChatRowOnce(messages, row.copy(moderationNotice = withKnownTargetName(messages, incoming)))
    val merged = if (incoming.fromIrc) {
        shown.copy(fromIrc = true, timeoutSeconds = incoming.timeoutSeconds)
    } else {
        shown.copy(moderatorName = incoming.moderatorName, targetName = incoming.targetName)
    }
    return messages.mapIndexed { position, message ->
        if (position == index) message.copy(moderationNotice = merged) else message
    }
}

/** Only a line the other source added and has not filled yet. */
private fun sameAction(shown: ChatModerationNotice, incoming: ChatModerationNotice): Boolean =
    shown.targetLogin == incoming.targetLogin &&
        (shown.timeoutSeconds == null) == (incoming.timeoutSeconds == null) &&
        if (incoming.fromIrc) !shown.fromIrc else shown.moderatorName == null

/** IRC names the chatter by login; their own messages above carry the display name. */
private fun withKnownTargetName(messages: List<ChatMessage>, notice: ChatModerationNotice): ChatModerationNotice {
    if (!notice.fromIrc) return notice
    val displayName = messages.lastOrNull { it.userLogin == notice.targetLogin && it.displayName.isNotBlank() }
        ?.displayName
        ?: return notice
    return notice.copy(targetName = displayName)
}
