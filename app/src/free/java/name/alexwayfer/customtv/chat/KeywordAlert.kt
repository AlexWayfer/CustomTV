package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.KeywordPhrase

/** Alerts for chosen words are a Premium feature; the free build never matches a keyword. */
@Suppress("unused")
internal fun messageMatchesKeyword(
    message: ChatMessage,
    phrases: List<KeywordPhrase>,
    selfLogin: String,
): Boolean = false
