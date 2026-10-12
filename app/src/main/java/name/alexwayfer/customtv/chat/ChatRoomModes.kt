package name.alexwayfer.customtv.chat

internal data class ChatRoomModes(
    val followersOnlyMinutes: Int? = null,
    val subscribersOnly: Boolean = false,
    val emoteOnly: Boolean = false,
    val slowSeconds: Int? = null,
)

sealed interface FollowForChatMode {
    data object Unknown : FollowForChatMode
    data object NotFollowing : FollowForChatMode
    data class Following(val atMillis: Long) : FollowForChatMode
}

sealed interface SubForChatMode {
    data object Unknown : SubForChatMode
    data object NotSubscribed : SubForChatMode
    data object Subscribed : SubForChatMode
}

internal sealed interface ChatModePlaque {
    data class FollowersNeedFollow(val channel: String) : ChatModePlaque
    data class FollowersWait(val remainingMillis: Long) : ChatModePlaque
    data object FollowersRoom : ChatModePlaque
    data class Subscribers(val offerSubscribe: Boolean) : ChatModePlaque
    data object Emotes : ChatModePlaque
    data class Slow(val seconds: Int, val remainingMillis: Long?) : ChatModePlaque
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

/** Who reads the chat: the broadcaster, or a viewer who may follow or subscribe to the channel. */
internal data class ChatModeViewer(
    val isBroadcaster: Boolean,
    val follow: FollowForChatMode,
    val subscription: SubForChatMode,
)

internal fun chatModePlaques(
    modes: ChatRoomModes,
    viewer: ChatModeViewer,
    nowMillis: Long,
    slowDeadlineMillis: Long?,
    channelName: String,
): List<ChatModePlaque> {
    return buildList {
        if (viewer.isBroadcaster) {
            if (modes.followersOnlyMinutes != null) add(ChatModePlaque.FollowersRoom)
        } else {
            followersPlaque(modes.followersOnlyMinutes, viewer.follow, nowMillis, channelName)?.let(::add)
        }
        if (modes.subscribersOnly && (viewer.isBroadcaster || viewer.subscription == SubForChatMode.NotSubscribed)) {
            add(ChatModePlaque.Subscribers(offerSubscribe = !viewer.isBroadcaster))
        }
        if (modes.emoteOnly) add(ChatModePlaque.Emotes)
        slowPlaque(
            modes.slowSeconds,
            if (viewer.isBroadcaster) null else slowDeadlineMillis,
            nowMillis,
        )?.let(::add)
    }
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
