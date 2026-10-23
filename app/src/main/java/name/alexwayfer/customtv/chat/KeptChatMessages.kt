package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.KeywordPhrase

/** The most rows a chat keeps, hidden ones included: room for [MAX_CHAT_MESSAGES] shown among hidden ones. */
internal const val MAX_KEPT_CHAT_MESSAGES = 500

/**
 * The newest of [messages] a chat keeps: [MAX_CHAT_MESSAGES] that [hidden] lets show and the hidden ones between them,
 * never more than [MAX_KEPT_CHAT_MESSAGES] in all.
 */
internal fun keptChatMessages(messages: List<ChatMessage>, hidden: (ChatMessage) -> Boolean): List<ChatMessage> {
    var start = messages.size
    var shown = 0
    while (start > 0 && shown < MAX_CHAT_MESSAGES && messages.size - start < MAX_KEPT_CHAT_MESSAGES) {
        start--
        if (!hidden(messages[start])) shown++
    }
    // A copy, so the kept rows do not hold the whole list they came from.
    return if (start == 0) messages else messages.subList(start, messages.size).toList()
}

/** What the chat shows of the [kept] rows: the ones [phrases] do not hide, no more than a chat shows. */
internal fun shownChatMessages(
    kept: List<ChatMessage>,
    phrases: List<KeywordPhrase>,
    selfLogin: String,
): List<ChatMessage> {
    val shown = withoutHiddenMessages(kept, phrases, selfLogin)
    return if (shown.size <= MAX_CHAT_MESSAGES) shown else shown.takeLast(MAX_CHAT_MESSAGES)
}

/** Cuts a live chat to the rows it keeps, by the phrases that hide messages in its channel now. */
internal class ChatTrim {
    private var phrases = emptyList<KeywordPhrase>()
    private var selfLogin = ""

    fun setHidden(phrases: List<KeywordPhrase>, selfLogin: String) {
        this.phrases = phrases
        this.selfLogin = selfLogin
    }

    fun keep(messages: List<ChatMessage>): List<ChatMessage> =
        keptChatMessages(messages) { messageHidden(it, phrases, selfLogin) }
}
