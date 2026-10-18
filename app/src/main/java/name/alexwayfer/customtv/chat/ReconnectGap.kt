package name.alexwayfer.customtv.chat

private val CONNECTION_NOTICES = setOf(
    ChatNotice.Connecting,
    ChatNotice.Welcome,
    ChatNotice.Disconnected,
    ChatNotice.Reconnecting,
    ChatNotice.Connected,
)

/**
 * The Disconnected notice of the outage the chat has just come back from, shown with Connected last; null when the
 * latest connection notice is not Connected or the connection before it was not lost on screen.
 */
internal fun reconnectGapNoticeId(messages: List<ChatMessage>): String? {
    val connection = messages.filter { it.notice in CONNECTION_NOTICES }
    if (connection.lastOrNull()?.notice != ChatNotice.Connected) return null
    val start = connection.dropLast(1).lastOrNull {
        it.notice != ChatNotice.Reconnecting && it.notice != ChatNotice.Connecting
    } ?: return null
    return start.id.takeIf { start.notice == ChatNotice.Disconnected }
}

internal data class ReconnectGapMerge(
    val messages: List<ChatMessage>,
    val chatters: Map<String, String>,
    /** The rows put back, oldest first. */
    val added: List<ChatMessage>,
)

/**
 * Puts back what the chat missed while its connection was down: the messages of [recent], history loaded once it
 * reconnected, that it does not show, from the last message before the [disconnectId] notice on. They go right
 * after that notice, oldest first. A chatter's first message of the session can be among them; a later live
 * message that took the mark loses it. Null when the notice is gone or nothing was missed.
 */
internal fun mergeReconnectGap(
    messages: List<ChatMessage>,
    recent: List<ChatMessage>,
    disconnectId: String,
    chatters: Map<String, String>,
    maxMessages: Int,
): ReconnectGapMerge? {
    val disconnectAt = messages.indexOfFirst { it.id == disconnectId }
    if (disconnectAt < 0) return null
    val before = messages.subList(0, disconnectAt)
    val lastBefore = before.lastOrNull { it.notice == null && it.userLogin.isNotBlank() } ?: before.lastOrNull()
    val since = lastBefore?.timestampMillis ?: Long.MIN_VALUE
    val shownIds = messages.asSequence().map { it.id }.toHashSet()
    val missed = recent.asSequence()
        .filter { it.id !in shownIds && it.timestampMillis >= since }
        .distinctBy { it.id }
        .sortedBy { it.timestampMillis }
        .toList()
    if (missed.isEmpty()) return null
    val after = messages.subList(disconnectAt + 1, messages.size)
    val firstAfter = after.asSequence()
        .filter { it.firstInSession }
        .mapNotNull { it.sessionAuthorLogin() }
        .toSet()
    var known = chatters - firstAfter
    val added = missed.map { message ->
        val update = rememberSessionChatter(known, message)
        known = update.chatters
        if (update.firstInSession) message.copy(firstInSession = true) else message
    }
    val moved = added.asSequence()
        .filter { it.firstInSession }
        .mapNotNull { it.sessionAuthorLogin() }
        .filter { it in firstAfter }
        .toSet()
    val later = if (moved.isEmpty()) after else after.map { it.withoutMovedFirst(moved) }
    return ReconnectGapMerge(
        messages = (before + messages[disconnectAt] + added + later).takeLast(maxMessages),
        chatters = known + chatters,
        added = added,
    )
}
