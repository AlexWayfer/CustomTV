package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatBadge

/** Moderator tools are Premium: the free chatter card only knows whether to show them locked. */
internal class ChatterModTools(val locked: Boolean = false)

private val MODERATOR_BADGES = setOf("broadcaster", "lead_moderator", "moderator")

/**
 * A moderator of the channel sees Moderation locked on someone else's card, as Premium would offer it there. Twitch
 * takes no action on the broadcaster, so their card has none. [ownBadges] are the user's own in this channel.
 */
internal fun chatterCardModerationLocked(
    ownBadges: List<ChatBadge>?,
    broadcasterId: String?,
    targetUserId: String?,
): Boolean = ownBadges.orEmpty().any { it.setId in MODERATOR_BADGES } &&
    !targetUserId.isNullOrBlank() &&
    targetUserId != broadcasterId
