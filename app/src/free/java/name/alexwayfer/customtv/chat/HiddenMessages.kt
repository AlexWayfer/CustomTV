package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.KeywordPhrase

/** Hiding messages by phrase is a Premium feature; the free build hides nothing. */
internal fun channelHiddenPhrases(
    phrases: List<KeywordPhrase>,
    hideWhenModerator: Boolean,
    moderates: Boolean,
): List<KeywordPhrase> = emptyList()

@Suppress("FunctionOnlyReturningConstant")
internal fun messageHidden(message: ChatMessage, phrases: List<KeywordPhrase>, selfLogin: String): Boolean = false

internal fun withoutHiddenMessages(
    messages: List<ChatMessage>,
    phrases: List<KeywordPhrase>,
    selfLogin: String,
): List<ChatMessage> = messages
