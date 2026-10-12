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
        byId[next.id] = byId[next.id]?.let { existing -> mergedThreadEntry(existing, next) } ?: next
    }
    return byId.values.sortedWith(
        compareBy<ChatThreadEntry> { if (it.id == starterId) 0 else 1 }
            .thenBy { it.timestampMillis }
            .thenBy { it.id },
    )
}

/** One entry known twice: the full message wins over a reply's quote, and each keeps what the other lacks. */
private fun mergedThreadEntry(existing: ChatThreadEntry, next: ChatThreadEntry): ChatThreadEntry = when {
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

internal fun ChatMessage.threadStarterId(): String {
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
