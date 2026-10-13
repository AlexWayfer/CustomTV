package name.alexwayfer.customtv.chat

internal data class ChatNick(
    val login: String,
    val displayName: String,
)

internal data class NickQuery(
    val start: Int,
    val end: Int,
    val text: String,
)

internal data class NickCompletionEdit(
    val text: String,
    val cursor: Int,
)

internal data class SessionChatterUpdate(
    val chatters: Map<String, String>,
    val firstInSession: Boolean,
)

internal fun rememberSessionChatter(
    current: Map<String, String>,
    message: ChatMessage,
): SessionChatterUpdate {
    val login = message.sessionAuthorLogin() ?: return SessionChatterUpdate(current, false)
    val displayName = message.displayName.trim().ifBlank { message.userLogin.trim() }
    val known = current[login] ?: return SessionChatterUpdate(current + (login to displayName), firstInSession = true)
    if (known == displayName) return SessionChatterUpdate(current, false)
    return SessionChatterUpdate(current + (login to displayName), firstInSession = false)
}

/** The lowercase login a message counts for as a chatter of the session; null for a system line. */
internal fun ChatMessage.sessionAuthorLogin(): String? {
    if (!countsAsSessionAuthor()) return null
    // A PubSub reward with text is followed by its IRC copy, which is the message that counts.
    if (RewardMessageDeduper.isPubSub(this) && rawText.isNotBlank()) return null
    return userLogin.trim().lowercase().ifEmpty { null }
}

internal fun nickQueryAtCursor(text: String, cursor: Int): NickQuery? {
    if (cursor !in 0..text.length) return null
    var start = cursor
    while (start > 0 && text[start - 1].isNickChar()) start--
    var end = cursor
    while (end < text.length && text[end].isNickChar()) end++
    val at = start - 1
    // An `@` that starts a word, before the cursor; one inside a word such as an email is not a mention.
    val mention = at >= 0 && text[at] == '@' && !(at > 0 && text[at - 1].isNickChar()) && cursor > at
    if (!mention || end == start) return null
    return NickQuery(start = at, end = end, text = text.substring(start, end))
}

internal fun matchingSessionNicks(
    chatters: Map<String, String>,
    query: String,
    channelOwner: ChatNick? = null,
    lastSpokeAt: Map<String, Long> = emptyMap(),
): List<ChatNick> {
    if (query.isEmpty()) return emptyList()
    // Whoever wrote last comes first; chatters with the same time, or none, go by name.
    val order = compareByDescending<ChatNick> { lastSpokeAt[it.login] ?: Long.MIN_VALUE }.then(nickAlphabet)
    val matches = chatters
        .mapNotNull { (login, displayName) ->
            ChatNick(login, displayName).takeIf { it.matchesNickQuery(query) }
        }
        .sortedWith(order)
    val owner = channelOwner
        ?.takeIf { it.login.isNotBlank() && it.matchesNickQuery(query) }
        ?: return matches
    val known = matches.firstOrNull { it.login.equals(owner.login, ignoreCase = true) } ?: owner
    return listOf(known) + matches.filterNot { it.login.equals(owner.login, ignoreCase = true) }
}

private val nickAlphabet = compareBy<ChatNick> { it.displayName.lowercase() }
    .thenBy { it.login.lowercase() }

private fun ChatNick.matchesNickQuery(query: String): Boolean {
    return displayName.startsWith(query, ignoreCase = true) ||
        login.startsWith(query, ignoreCase = true)
}

internal fun textWithCompletedNick(
    text: String,
    cursor: Int,
    displayName: String,
    maxLength: Int,
): NickCompletionEdit? {
    val query = nickQueryAtCursor(text, cursor) ?: return null
    val name = displayName.trim()
    if (name.isEmpty()) return null
    val mention = "@$name"
    val alreadySpaced = query.end < text.length && text[query.end] == ' '
    val insert = if (alreadySpaced) mention else "$mention "
    val next = text.substring(0, query.start) + insert + text.substring(query.end)
    if (next.length > maxLength) return null
    return NickCompletionEdit(text = next, cursor = query.start + mention.length + 1)
}
