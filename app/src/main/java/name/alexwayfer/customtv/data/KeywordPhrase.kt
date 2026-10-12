package name.alexwayfer.customtv.data

/** A word or phrase that alerts like a mention: anywhere in the text, or only as a whole word. */
data class KeywordPhrase(val text: String, val wholeWord: Boolean)
