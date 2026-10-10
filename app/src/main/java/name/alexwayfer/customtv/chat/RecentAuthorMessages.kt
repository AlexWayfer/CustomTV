package name.alexwayfer.customtv.chat

internal const val MAX_RECENT_AUTHOR_MESSAGES = 20

internal fun rememberRecentAuthorMessages(
    history: Map<String, List<ChatMessage>>,
    previous: List<ChatMessage>,
    updated: List<ChatMessage>,
): Map<String, List<ChatMessage>> {
    val previousById = previous.associateBy(ChatMessage::id)
    var result = history
    updated.forEach { message ->
        if (!message.isAuthorHistoryMessage() || previousById[message.id] == message) return@forEach
        val login = message.userLogin.lowercase()
        val current = result[login].orEmpty()
        val index = current.indexOfFirst { it.id == message.id }
        val next = if (index >= 0) {
            current.toMutableList().apply { this[index] = message }
        } else {
            (current + message).takeLast(MAX_RECENT_AUTHOR_MESSAGES)
        }
        if (next != current) result = result + (login to next)
    }
    return result
}

/** Appends [added] in order, for a chat that only grows, without diffing a visible window. */
internal fun appendRecentAuthorMessages(
    history: Map<String, List<ChatMessage>>,
    added: List<ChatMessage>,
    transform: (ChatMessage) -> ChatMessage,
): Map<String, List<ChatMessage>> {
    if (added.none { it.isAuthorHistoryMessage() }) return history
    val result = history.toMutableMap()
    added.forEach { message ->
        if (!message.isAuthorHistoryMessage()) return@forEach
        val login = message.userLogin.lowercase()
        result[login] = (result[login].orEmpty() + transform(message)).takeLast(MAX_RECENT_AUTHOR_MESSAGES)
    }
    return result
}

/**
 * Puts chat history loaded on joining ahead of what each author wrote live, keeping the newest
 * [MAX_RECENT_AUTHOR_MESSAGES]. Live messages of [movedFirstLogins] drop their first mark, which moved
 * to the history.
 */
internal fun prependRecentAuthorMessages(
    history: Map<String, List<ChatMessage>>,
    recent: List<ChatMessage>,
    movedFirstLogins: Set<String>,
): Map<String, List<ChatMessage>> {
    val byLogin = recent.filter { it.isAuthorHistoryMessage() }.groupBy { it.userLogin.lowercase() }
    if (byLogin.isEmpty() && movedFirstLogins.isEmpty()) return history
    val result = history.toMutableMap()
    (byLogin.keys + movedFirstLogins).forEach { login ->
        val earlier = byLogin[login].orEmpty()
        val earlierIds = earlier.asSequence().map { it.id }.toHashSet()
        val live = result[login].orEmpty()
            .filter { it.id !in earlierIds }
            .map { it.withoutMovedFirst(movedFirstLogins) }
        result[login] = (earlier + live).takeLast(MAX_RECENT_AUTHOR_MESSAGES)
    }
    return result
}

internal fun markRecentAuthorMessageDeleted(
    history: Map<String, List<ChatMessage>>,
    messageId: String,
    deletedBy: String?,
): Map<String, List<ChatMessage>> = history.mapValues { (_, messages) ->
    markMessageDeleted(messages, messageId, deletedBy)
}

internal fun markRecentAuthorMessagesDeleted(
    history: Map<String, List<ChatMessage>>,
    userLogin: String,
    deletedBy: String?,
    timeoutSeconds: Long?,
    banned: Boolean,
): Map<String, List<ChatMessage>> {
    val login = userLogin.lowercase()
    val messages = history[login] ?: return history
    return history + (login to markUserMessagesDeleted(messages, login, deletedBy, timeoutSeconds, banned))
}

private fun ChatMessage.isAuthorHistoryMessage(): Boolean =
    userLogin.isNotBlank() && notice == null && when (eventKind) {
        ChatEventKind.Normal,
        ChatEventKind.Announcement,
        ChatEventKind.Subscription,
        ChatEventKind.Highlight,
        ChatEventKind.Reward,
        ChatEventKind.WatchStreak -> true
        else -> false
    }
