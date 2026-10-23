package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.KeywordPhrase

internal const val MAX_RECENT_MENTIONS = MAX_RECENT_AUTHOR_MESSAGES

/** Each chatter's recent messages in this chat, and the recent messages that mention them, by lowercase login. */
internal data class ChatterHistory(
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val mentions: Map<String, List<ChatMessage>> = emptyMap(),
) {
    fun map(transform: (ChatMessage) -> ChatMessage): ChatterHistory = ChatterHistory(
        messages = messages.mapValues { (_, list) -> list.map(transform) },
        mentions = mentions.mapValues { (_, list) -> list.map(transform) },
    )
}

/** The signed-in user, whose name, display name, and keywords also make a message their mention. */
internal data class ChatMentionSelf(
    val login: String = "",
    val displayName: String = "",
    val keywordPhrases: List<KeywordPhrase> = emptyList(),
)

/**
 * The lowercase logins [message] mentions: the author it replies to, an `@login`, a bare login of a chatter in
 * [knownLogins], and the signed-in [self] by name, display name, or keyword. Never the message's own author.
 */
internal fun mentionedLogins(message: ChatMessage, knownLogins: Set<String>, self: ChatMentionSelf): Set<String> {
    if (!message.isAuthorHistoryMessage() || !message.isMentionableChat()) return emptySet()
    val logins = buildSet {
        message.reply?.parentUserLogin?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let(::add)
        findChatNicks(message.rawText, knownLogins).forEach { add(it.key) }
        if (self.login.isNotBlank() && (
                messageMentionsUser(message, self.login, self.displayName) ||
                    messageMatchesKeyword(message, self.keywordPhrases, self.login)
                )
        ) {
            add(self.login.lowercase())
        }
    }
    return logins - message.userLogin.lowercase()
}

internal fun rememberChatterHistory(
    history: ChatterHistory,
    previous: List<ChatMessage>,
    updated: List<ChatMessage>,
    self: ChatMentionSelf,
): ChatterHistory {
    val messages = rememberRecentAuthorMessages(history.messages, previous, updated)
    val previousById = previous.associateBy(ChatMessage::id)
    val changed = updated.filter { previousById[it.id] != it }
    return ChatterHistory(messages, withMentions(history.mentions, changed, messages.keys, self))
}

/** Appends [added] in order, for a chat that only grows, without diffing a visible window. */
internal fun appendChatterHistory(
    history: ChatterHistory,
    added: List<ChatMessage>,
    self: ChatMentionSelf,
    transform: (ChatMessage) -> ChatMessage,
): ChatterHistory {
    val messages = appendRecentAuthorMessages(history.messages, added, transform)
    return ChatterHistory(messages, withMentions(history.mentions, added, messages.keys, self, transform))
}

/**
 * Puts chat history loaded on joining ahead of what was written live, for each author and each mentioned
 * chatter, keeping the newest. Live messages of [movedFirstLogins] drop their first mark, which moved to the history.
 */
internal fun prependChatterHistory(
    history: ChatterHistory,
    recent: List<ChatMessage>,
    movedFirstLogins: Set<String>,
    self: ChatMentionSelf,
): ChatterHistory {
    val messages = prependRecentAuthorMessages(history.messages, recent, movedFirstLogins)
    val earlierMentions = withMentions(emptyMap(), recent, messages.keys, self)
    if (earlierMentions.isEmpty() && movedFirstLogins.isEmpty()) return ChatterHistory(messages, history.mentions)
    val mentions = history.mentions.toMutableMap()
    (earlierMentions.keys + history.mentions.keys).forEach { login ->
        val earlier = earlierMentions[login].orEmpty()
        val earlierIds = earlier.asSequence().map { it.id }.toHashSet()
        val live = history.mentions[login].orEmpty()
            .filter { it.id !in earlierIds }
            .map { it.withoutMovedFirst(movedFirstLogins) }
        mentions[login] = (earlier + live).takeLast(MAX_RECENT_MENTIONS)
    }
    return ChatterHistory(messages, mentions)
}

internal fun markChatterMessageDeleted(history: ChatterHistory, messageId: String, deletedBy: String?): ChatterHistory =
    ChatterHistory(
        messages = markRecentAuthorMessageDeleted(history.messages, messageId, deletedBy),
        mentions = history.mentions.mapValues { (_, list) -> markMessageDeleted(list, messageId, deletedBy) },
    )

/** Marks what a moderator cleared of [userLogin]: their own messages, and their mentions of others. */
internal fun markChatterMessagesDeleted(
    history: ChatterHistory,
    userLogin: String,
    deletedBy: String?,
    timeoutSeconds: Long?,
    banned: Boolean,
): ChatterHistory {
    val login = userLogin.lowercase()
    return ChatterHistory(
        messages = markRecentAuthorMessagesDeleted(history.messages, login, deletedBy, timeoutSeconds, banned),
        mentions = history.mentions.mapValues { (_, list) ->
            markUserMessagesDeleted(list, login, deletedBy, timeoutSeconds, banned)
        },
    )
}

/** Adds each of [messages] to the mentions of every login it names, or updates the copy already there. */
private fun withMentions(
    mentions: Map<String, List<ChatMessage>>,
    messages: List<ChatMessage>,
    knownLogins: Set<String>,
    self: ChatMentionSelf,
    transform: (ChatMessage) -> ChatMessage = { it },
): Map<String, List<ChatMessage>> {
    val mentioning = messages.mapNotNull { message ->
        mentionedLogins(message, knownLogins, self).takeIf { it.isNotEmpty() }?.let { logins -> message to logins }
    }
    if (mentioning.isEmpty()) return mentions
    val result = mentions.toMutableMap()
    mentioning.forEach { (message, logins) ->
        val shown = transform(message)
        logins.forEach { login ->
            val current = result[login].orEmpty()
            val index = current.indexOfFirst { it.id == shown.id }
            result[login] = if (index >= 0) {
                current.toMutableList().apply { this[index] = shown }
            } else {
                (current + shown).takeLast(MAX_RECENT_MENTIONS)
            }
        }
    }
    return result
}
