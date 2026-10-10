package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

/** A word or phrase that alerts like a mention: anywhere in the text, or only as a whole word. */
data class KeywordPhrase(val text: String, val wholeWord: Boolean)

internal fun keywordPhrasesAfterAdd(
    current: List<KeywordPhrase>,
    typed: String,
    wholeWord: Boolean,
): List<KeywordPhrase> {
    val text = typed.trim()
    if (text.isEmpty()) return current
    if (current.any { it.text.equals(text, ignoreCase = true) }) return current
    return current + KeywordPhrase(text, wholeWord)
}

internal fun keywordPhrasesAfterRemove(current: List<KeywordPhrase>, text: String): List<KeywordPhrase> {
    return current.filterNot { it.text.equals(text, ignoreCase = true) }
}

internal fun keywordPhrasesAfterWholeWordChange(
    current: List<KeywordPhrase>,
    text: String,
    wholeWord: Boolean,
): List<KeywordPhrase> {
    return current.map { if (it.text.equals(text, ignoreCase = true)) it.copy(wholeWord = wholeWord) else it }
}

internal fun encodeKeywordPhrases(phrases: List<KeywordPhrase>): String {
    return JSONArray().apply {
        phrases.forEach { put(JSONObject().put("text", it.text).put("wholeWord", it.wholeWord)) }
    }.toString()
}

/** A plain string is a phrase saved before the whole-word choice existed; it matched whole words. */
internal fun decodeKeywordPhrases(raw: String?): List<KeywordPhrase> {
    if (raw.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    var phrases = emptyList<KeywordPhrase>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index)
        phrases = if (item != null) {
            keywordPhrasesAfterAdd(phrases, item.optString("text"), item.optBoolean("wholeWord"))
        } else {
            keywordPhrasesAfterAdd(phrases, array.optString(index), wholeWord = true)
        }
    }
    return phrases
}
