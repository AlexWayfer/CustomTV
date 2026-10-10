package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChatSendResult

/**
 * A channel's warning the user has to acknowledge before chatting there again. A refused send tells only that there
 * is one, so its reason and rules stay empty.
 */
internal data class ChatWarning(
    val reason: String = "",
    val citedRules: List<String> = emptyList(),
)

/** Twitch's drop code for a message from a user who has not acknowledged a warning in the channel. */
private const val WARNED_DROP_CODE = "user_warned"

/** Whether Twitch dropped the message because the user has a warning to acknowledge first. */
internal fun chatSendWarned(result: ChatSendResult): Boolean =
    result is ChatSendResult.Dropped && result.code == WARNED_DROP_CODE

/** How long Acknowledge waits after the warning appears, so the user reads it first. */
private const val WARNING_READ_MILLIS = 7_000L

/** The whole seconds Acknowledge still waits, rounded up so the last second still shows; 0 once it can be pressed. */
internal fun warningAcknowledgeWaitSeconds(shownAtMillis: Long, nowMillis: Long): Int =
    ((shownAtMillis + WARNING_READ_MILLIS - nowMillis + 999) / 1_000).coerceAtLeast(0).toInt()