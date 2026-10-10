package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

internal data class ChatThreadEntry(
    val id: String,
    val login: String,
    val displayName: String,
    val body: String,
    val timestampMillis: Long,
    val isAction: Boolean = false,
    val color: Color = Color.Unspecified,
    val message: ChatMessage? = null,
)

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

internal fun replyParentMessageId(session: ChatReplySession?): String? = session?.targetId

internal data class ChatThreadWindow(
    val pinned: ChatThreadEntry?,
    val following: List<ChatThreadEntry>,
)

internal fun threadWindow(entries: List<ChatThreadEntry>, starterId: String): ChatThreadWindow {
    val pinned = entries.firstOrNull { it.id == starterId }
    val following = if (pinned == null) entries else entries.filter { it.id != starterId }
    return ChatThreadWindow(pinned = pinned, following = following)
}

internal fun threadPanelDragCloses(dragPx: Float, heightPx: Int, slopPx: Float): Boolean {
    if (dragPx <= 0f || heightPx <= 0) return false
    val limit = minOf(slopPx, heightPx * 0.35f).coerceAtLeast(1f)
    return dragPx >= limit
}

/**
 * The height the pinned starter may take in a thread body of [availablePx]. When the starter and the
 * replies do not both fit, the starter keeps what the replies leave free, but never less than half,
 * so neither part is pushed out and each scrolls in its own share. A height of 0 means not measured.
 */
internal fun threadPinnedMaxHeightPx(availablePx: Int, pinnedPx: Int, followingPx: Int): Int {
    if (pinnedPx <= 0 || followingPx <= 0) return availablePx
    if (pinnedPx + followingPx <= availablePx) return pinnedPx
    return minOf(pinnedPx, maxOf(availablePx / 2, availablePx - followingPx))
}

/** While replying with the keyboard or emote picker up, the chat mode plaques give their room to the thread. */
internal fun threadReplyHidesChrome(replying: Boolean, composerOpen: Boolean): Boolean = replying && composerOpen

internal fun ChatThreadEntry.asChatMessage(): ChatMessage {
    message?.let { return it }
    return ChatMessage(
        id = id,
        userLogin = login,
        displayName = displayName,
        color = color,
        rawText = body,
        parts = if (body.isEmpty()) emptyList() else listOf(ChatPart.Text(body)),
        timestampMillis = timestampMillis,
        isAction = isAction,
    )
}

internal fun mergeThreadEntries(
    starterId: String,
    current: List<ChatThreadEntry>,
    incoming: List<ChatThreadEntry>,
): List<ChatThreadEntry> {
    val byId = LinkedHashMap<String, ChatThreadEntry>()
    current.forEach { byId[it.id] = it }
    incoming.forEach { next ->
        val existing = byId[next.id]
        byId[next.id] = when {
            existing == null -> next
            existing.message == null && next.message != null -> next.copy(
                body = next.body.ifBlank { existing.body },
                color = next.color.takeUnless { it == Color.Unspecified } ?: existing.color,
                timestampMillis = next.timestampMillis.takeIf { it > 0L } ?: existing.timestampMillis,
            )
            existing.body.isBlank() && next.body.isNotBlank() -> next.copy(
                color = existing.color.takeUnless { it == Color.Unspecified } ?: next.color,
                message = next.message ?: existing.message,
                timestampMillis = next.timestampMillis.takeIf { it > 0L } ?: existing.timestampMillis,
            )
            existing.timestampMillis == 0L && next.timestampMillis > 0L ->
                existing.copy(timestampMillis = next.timestampMillis)
            else -> existing
        }
    }
    return byId.values.sortedWith(
        compareBy<ChatThreadEntry> { if (it.id == starterId) 0 else 1 }
            .thenBy { it.timestampMillis }
            .thenBy { it.id },
    )
}

internal fun threadEntriesFromKnown(
    starterId: String,
    clicked: ChatMessage?,
    known: List<ChatMessage>,
): List<ChatThreadEntry> {
    val entries = known.mapNotNull { message ->
        if (message.id == starterId || message.threadStarterId() == starterId) threadEntry(message) else null
    }.toMutableList()
    if (clicked != null && entries.none { it.id == clicked.id }) entries += threadEntry(clicked)
    if (entries.none { it.id == starterId }) {
        clicked?.reply?.let { reply -> syntheticStarter(starterId, reply) }?.let(entries::add)
    }
    return entries
}

private fun ChatMessage.threadStarterId(): String {
    return reply?.threadParentMsgId?.takeIf { it.isNotBlank() }
        ?: reply?.parentMsgId
        ?: id
}

private fun threadEntry(message: ChatMessage): ChatThreadEntry {
    return ChatThreadEntry(
        id = message.id,
        login = message.userLogin,
        displayName = message.displayName.ifBlank { message.userLogin },
        body = message.rawText,
        timestampMillis = message.timestampMillis,
        isAction = message.isAction,
        color = message.color,
        message = message,
    )
}

private fun syntheticStarter(starterId: String, reply: ChatReply): ChatThreadEntry? {
    val parentIsStarter = reply.parentMsgId == starterId
    val login = when {
        parentIsStarter -> reply.parentUserLogin
        reply.threadParentUserLogin.isNotBlank() -> reply.threadParentUserLogin
        else -> return null
    }
    val displayName = if (parentIsStarter) reply.parentDisplayName.ifBlank { login } else login
    return ChatThreadEntry(
        id = starterId,
        login = login,
        displayName = displayName,
        body = if (parentIsStarter) reply.parentBody else "",
        timestampMillis = 0L,
        isAction = parentIsStarter && reply.parentIsAction,
    )
}
