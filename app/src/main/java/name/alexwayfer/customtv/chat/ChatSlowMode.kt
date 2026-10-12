package name.alexwayfer.customtv.chat

import kotlin.time.Duration.Companion.seconds

private val SLOW_MODE_EXEMPT_BADGES = setOf("broadcaster", "lead_moderator", "moderator", "vip")

internal fun slowModeDeadline(sentAtMillis: Long, slowSeconds: Int): Long =
    sentAtMillis + slowSeconds.seconds.inWholeMilliseconds

/**
 * Whether [message] is the user's own chat line, whichever device sent it. Only such a line starts
 * the slow mode countdown, so this device's own send counts once, when its echo arrives.
 */
internal fun isOwnLiveChatLine(message: ChatMessage, ownUserId: String?): Boolean =
    !ownUserId.isNullOrBlank() &&
        message.userId == ownUserId &&
        message.eventKind == ChatEventKind.Normal &&
        message.notice == null

/**
 * The slow mode deadline after the user's own [message] arrives, or null when slow mode does not
 * apply to them: the broadcaster, moderators, and VIPs, as the badges on that message show.
 */
internal fun slowModeDeadlineAfterOwnMessage(
    message: ChatMessage,
    slowSeconds: Int?,
    viewerIsBroadcaster: Boolean,
    receivedAtMillis: Long,
): Long? {
    if (viewerIsBroadcaster) return null
    if (message.badges.any { it.setId in SLOW_MODE_EXEMPT_BADGES }) return null
    val seconds = slowSeconds?.takeIf { it > 0 } ?: return null
    return slowModeDeadline(receivedAtMillis, seconds)
}

/** Send stays blocked while the countdown runs and while this device's own message has not come back yet. */
internal fun slowModeBlocksSend(plaque: ChatModePlaque.Slow?, awaitingOwnEcho: Boolean): Boolean =
    plaque != null && (plaque.remainingMillis != null || awaitingOwnEcho)
