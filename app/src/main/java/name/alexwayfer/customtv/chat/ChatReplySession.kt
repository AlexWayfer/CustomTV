package name.alexwayfer.customtv.chat

internal data class ChatReplySession(
    val starterId: String,
    val targetId: String,
    val entries: List<ChatThreadEntry>,
)

private val TWITCH_MESSAGE_ID = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
)

internal fun chatMessageCanBeReplied(message: ChatMessage): Boolean {
    if (message.notice != null || message.userLogin.isBlank()) return false
    if (message.eventKind != ChatEventKind.Normal &&
        message.eventKind != ChatEventKind.Highlight &&
        message.eventKind != ChatEventKind.Reward
    ) {
        return false
    }
    return TWITCH_MESSAGE_ID.matches(message.id)
}

internal fun replySessionFor(clicked: ChatMessage, known: List<ChatMessage>): ChatReplySession? {
    if (!chatMessageCanBeReplied(clicked)) return null
    val starterId = clicked.threadStarterId()
    return ChatReplySession(
        starterId = starterId,
        targetId = clicked.id,
        entries = mergeThreadEntries(starterId, emptyList(), threadEntriesFromKnown(starterId, clicked, known)),
    )
}

internal fun replySessionChooses(session: ChatReplySession, messageId: String): ChatReplySession {
    if (session.entries.none { it.id == messageId }) return session
    return session.copy(targetId = messageId)
}

internal fun replySessionCancelsToStarter(session: ChatReplySession): ChatReplySession {
    return session.copy(targetId = session.starterId)
}

internal fun replySessionShowsReplyingTo(session: ChatReplySession): Boolean {
    return session.targetId != session.starterId
}

/** The thread marks the message being replied to, the starter too once a reply is cancelled, as Twitch does. */
internal fun replySessionMarksTarget(session: ChatReplySession, messageId: String): Boolean =
    messageId == session.targetId

internal fun replyParentMessageId(session: ChatReplySession?): String? = session?.targetId
