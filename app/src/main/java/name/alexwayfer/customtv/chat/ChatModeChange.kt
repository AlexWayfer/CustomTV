package name.alexwayfer.customtv.chat

/** A chat mode the streamer or a moderator turns on, off, or to a new value. */
internal sealed interface ChatModeChange {
    /** Slow mode with [seconds] between one chatter's messages; off when null. */
    data class Slow(val seconds: Int?) : ChatModeChange

    /** Followers-only chat for those who have followed for [minutes]; off when null. */
    data class Followers(val minutes: Int?) : ChatModeChange

    data class Subscribers(val on: Boolean) : ChatModeChange

    data class EmoteOnly(val on: Boolean) : ChatModeChange

    data class UniqueChat(val on: Boolean) : ChatModeChange
}

/** These modes with [change] made. */
internal fun ChatRoomModes.with(change: ChatModeChange): ChatRoomModes = when (change) {
    is ChatModeChange.Slow -> copy(slowSeconds = change.seconds)
    is ChatModeChange.Followers -> copy(followersOnlyMinutes = change.minutes)
    is ChatModeChange.Subscribers -> copy(subscribersOnly = change.on)
    is ChatModeChange.EmoteOnly -> copy(emoteOnly = change.on)
    is ChatModeChange.UniqueChat -> copy(uniqueChat = change.on)
}
