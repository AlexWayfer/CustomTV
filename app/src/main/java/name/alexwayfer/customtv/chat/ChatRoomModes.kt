package name.alexwayfer.customtv.chat

internal data class ChatRoomModes(
    val followersOnlyMinutes: Int? = null,
    val subscribersOnly: Boolean = false,
    val emoteOnly: Boolean = false,
    val slowSeconds: Int? = null,
)

sealed class FollowForChatMode {
    data object Unknown : FollowForChatMode()
    data object NotFollowing : FollowForChatMode()
    data class Following(val atMillis: Long) : FollowForChatMode()
}

sealed class SubForChatMode {
    data object Unknown : SubForChatMode()
    data object NotSubscribed : SubForChatMode()
    data object Subscribed : SubForChatMode()
}

internal sealed class TwitchPlaqueAction {
    data object LogIn : TwitchPlaqueAction()
    data class Open(val url: String) : TwitchPlaqueAction()
}

internal sealed class ChatModePlaque {
    data class FollowersNeedFollow(val channel: String) : ChatModePlaque()
    data class FollowersWait(val remainingMillis: Long) : ChatModePlaque()
    data object FollowersRoom : ChatModePlaque()
    data class Subscribers(val offerSubscribe: Boolean) : ChatModePlaque()
    data object Emotes : ChatModePlaque()
    data class Slow(val seconds: Int, val remainingMillis: Long?) : ChatModePlaque()
}

internal fun roomModesAfterConnect(current: ChatRoomModes, sameChannel: Boolean): ChatRoomModes =
    if (sameChannel) current else ChatRoomModes()

internal fun chatRoomModesAfter(current: ChatRoomModes, tags: Map<String, String>): ChatRoomModes {
    var next = current
    if ("followers-only" in tags) {
        next = next.copy(followersOnlyMinutes = followersOnlyMinutes(tags["followers-only"]))
    }
    if ("subs-only" in tags) {
        next = next.copy(subscribersOnly = tags["subs-only"] == "1")
    }
    if ("emote-only" in tags) {
        next = next.copy(emoteOnly = tags["emote-only"] == "1")
    }
    if ("slow" in tags) {
        next = next.copy(slowSeconds = slowSeconds(tags["slow"]))
    }
    return next
}

internal fun chatModePlaques(
    modes: ChatRoomModes,
    viewerIsBroadcaster: Boolean,
    follow: FollowForChatMode,
    subscription: SubForChatMode,
    nowMillis: Long,
    slowDeadlineMillis: Long?,
    channelName: String,
): List<ChatModePlaque> {
    return buildList {
        if (viewerIsBroadcaster) {
            if (modes.followersOnlyMinutes != null) add(ChatModePlaque.FollowersRoom)
        } else {
            followersPlaque(modes.followersOnlyMinutes, follow, nowMillis, channelName)?.let(::add)
        }
        if (modes.subscribersOnly && (viewerIsBroadcaster || subscription == SubForChatMode.NotSubscribed)) {
            add(ChatModePlaque.Subscribers(offerSubscribe = !viewerIsBroadcaster))
        }
        if (modes.emoteOnly) add(ChatModePlaque.Emotes)
        slowPlaque(
            modes.slowSeconds,
            if (viewerIsBroadcaster) null else slowDeadlineMillis,
            nowMillis,
        )?.let(::add)
    }
}

/** What a chat mode plaque's button is called. */
internal enum class ChatModeButton { Follow, Subscribe, LogIn }

/**
 * The button a plaque offers, if any. Logged out, following or subscribing starts with logging in, so the
 * button says Log in, which is what it does then.
 */
internal fun chatModeButton(plaque: ChatModePlaque, signedIn: Boolean): ChatModeButton? {
    val button = when (plaque) {
        is ChatModePlaque.FollowersNeedFollow -> ChatModeButton.Follow
        is ChatModePlaque.Subscribers if plaque.offerSubscribe -> ChatModeButton.Subscribe
        else -> return null
    }
    return if (signedIn) button else ChatModeButton.LogIn
}

internal fun followersPlaqueAction(signedIn: Boolean, channelLogin: String): TwitchPlaqueAction =
    twitchPlaqueAction(signedIn, twitchChannelPage(channelLogin))

internal fun subscribersPlaqueAction(signedIn: Boolean, channelLogin: String): TwitchPlaqueAction =
    twitchPlaqueAction(signedIn, twitchSubscribePage(channelLogin))

internal fun twitchPlaqueAction(signedIn: Boolean, url: String): TwitchPlaqueAction =
    if (!signedIn) TwitchPlaqueAction.LogIn else TwitchPlaqueAction.Open(url)

internal fun twitchChannelPage(login: String): String {
    return "https://www.twitch.tv/${twitchLoginPath(login)}"
}

internal fun twitchSubscribePage(login: String): String {
    return "https://www.twitch.tv/subs/${twitchLoginPath(login)}"
}

private fun twitchLoginPath(login: String): String = login.trim().removePrefix("#").lowercase()

internal fun slowModeDeadline(sentAtMillis: Long, slowSeconds: Int): Long =
    sentAtMillis + slowSeconds * 1_000L

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

private val SLOW_MODE_EXEMPT_BADGES = setOf("broadcaster", "lead_moderator", "moderator", "vip")

/** Send stays blocked while the countdown runs and while this device's own message has not come back yet. */
internal fun slowModeBlocksSend(plaque: ChatModePlaque.Slow?, awaitingOwnEcho: Boolean): Boolean =
    plaque != null && (plaque.remainingMillis != null || awaitingOwnEcho)

internal fun chatModeCountdown(remainingMillis: Long): String {
    val totalSeconds = ((remainingMillis + 999L) / 1_000L).coerceAtLeast(1L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        days > 0 -> "${days}d $hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        hours > 0 -> "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        else -> "$minutes:${seconds.twoDigits()}"
    }
}

internal fun plaqueTicks(plaque: ChatModePlaque): Boolean = when (plaque) {
    is ChatModePlaque.FollowersWait -> true
    is ChatModePlaque.Slow -> plaque.remainingMillis != null
    else -> false
}

private fun followersPlaque(
    minutes: Int?,
    follow: FollowForChatMode,
    nowMillis: Long,
    channelName: String,
): ChatModePlaque? {
    if (minutes == null) return null
    return when (follow) {
        FollowForChatMode.Unknown -> null
        FollowForChatMode.NotFollowing -> ChatModePlaque.FollowersNeedFollow(channelName)
        is FollowForChatMode.Following -> {
            val requiredMillis = minutes * 60_000L
            val remaining = follow.atMillis + requiredMillis - nowMillis
            if (remaining > 0L) ChatModePlaque.FollowersWait(remaining) else null
        }
    }
}

private fun slowPlaque(
    slowSeconds: Int?,
    slowDeadlineMillis: Long?,
    nowMillis: Long,
): ChatModePlaque.Slow? {
    val seconds = slowSeconds ?: return null
    if (seconds <= 0) return null
    val remaining = slowDeadlineMillis?.minus(nowMillis)?.takeIf { it > 0L }
    return ChatModePlaque.Slow(seconds, remaining)
}

private fun followersOnlyMinutes(raw: String?): Int? {
    val minutes = raw?.toIntOrNull() ?: return null
    return minutes.takeIf { it >= 0 }
}

private fun slowSeconds(raw: String?): Int? {
    val seconds = raw?.toIntOrNull() ?: return null
    return seconds.takeIf { it > 0 }
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
