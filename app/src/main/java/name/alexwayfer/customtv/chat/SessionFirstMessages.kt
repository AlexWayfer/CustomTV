package name.alexwayfer.customtv.chat

internal fun ChatMessage.countsAsSessionAuthor(): Boolean {
    if (notice != null || emoteChange != null) return false
    if (userLogin.isBlank()) return false
    return when (eventKind) {
        ChatEventKind.Normal,
        ChatEventKind.Highlight,
        ChatEventKind.Reward,
        ChatEventKind.Announcement,
        ChatEventKind.Subscription,
        ChatEventKind.WatchStreak,
        -> true
        else -> false
    }
}

internal data class RecentChatSessionMerge(
    /** The history messages the chat does not show yet, each chatter's earliest one marked first. */
    val recent: List<ChatMessage>,
    /** The chat, with the first mark taken off live messages whose author wrote earlier in [recent]. */
    val current: List<ChatMessage>,
    val chatters: Map<String, String>,
    /** Logins whose live first mark moved to [recent]. */
    val movedFirstLogins: Set<String>,
)

/**
 * Chat history loaded on joining comes before every live message, so a chatter's first message of
 * the session is their earliest one in the history. A live message marked first loses the mark to it.
 * A live display name stays: it is newer than the history's.
 */
internal fun mergeRecentChatSession(
    recent: List<ChatMessage>,
    current: List<ChatMessage>,
    chatters: Map<String, String>,
): RecentChatSessionMerge {
    val shownIds = current.asSequence().map { it.id }.toHashSet()
    var seen = emptyMap<String, String>()
    val marked = recent.filter { it.id !in shownIds }.map { message ->
        val update = rememberSessionChatter(seen, message)
        seen = update.chatters
        if (update.firstInSession) message.copy(firstInSession = true) else message
    }
    val moved = current.asSequence()
        .filter { it.firstInSession && it.userLogin.trim().lowercase() in seen }
        .map { it.userLogin.trim().lowercase() }
        .toSet()
    return RecentChatSessionMerge(
        recent = marked,
        current = if (moved.isEmpty()) current else current.map { it.withoutMovedFirst(moved) },
        chatters = seen + chatters,
        movedFirstLogins = moved,
    )
}

internal fun ChatMessage.withoutMovedFirst(logins: Set<String>): ChatMessage =
    if (firstInSession && userLogin.trim().lowercase() in logins) copy(firstInSession = false) else this
