package name.alexwayfer.customtv.chat

/** The newest message the signed-in user wrote in this chat; its badges are the current ones. */
internal fun latestOwnChatMessage(messages: List<ChatMessage>, selfLogin: String): ChatMessage? {
    if (selfLogin.isBlank()) return null
    return messages.lastOrNull { message ->
        message.notice == null && message.userLogin.equals(selfLogin, ignoreCase = true) && message.countsAsSessionAuthor()
    }
}

/**
 * The badges the user has now: those loaded at [loadedAtMillis], until the user writes in this chat
 * after that, since the message carries the badges as they are then, such as a new subscription.
 * Without a loaded answer, the newest own message is all there is.
 */
internal fun ownChatBadgesNow(loaded: List<ChatBadge>?, loadedAtMillis: Long, latestOwn: ChatMessage?): List<ChatBadge>? = when {
    latestOwn != null && (loaded == null || latestOwn.timestampMillis >= loadedAtMillis) -> latestOwn.badges
    else -> loaded
}
