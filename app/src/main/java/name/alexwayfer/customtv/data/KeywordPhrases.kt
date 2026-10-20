package name.alexwayfer.customtv.data

import org.json.JSONArray
import org.json.JSONObject

/** A phrase already saved, in any case, keeps its text and place and takes the new [wholeWord]. */
internal fun keywordPhrasesAfterAdd(
    current: List<KeywordPhrase>,
    typed: String,
    wholeWord: Boolean,
): List<KeywordPhrase> {
    val text = typed.trim()
    if (text.isEmpty()) return current
    if (current.any { it.text.equals(text, ignoreCase = true) }) {
        return keywordPhrasesAfterWholeWordChange(current, text, wholeWord)
    }
    return current + KeywordPhrase(text, wholeWord)
}

/**
 * Adds what was typed or pasted. A comma separates phrases, and `*phrase*` matches part of a word. A list
 * (more than one phrase) keeps the mode each item spells, so a pasted [keywordPhrasesExport] restores it;
 * a single phrase without stars takes [wholeWord] from the toggle.
 */
internal fun keywordPhrasesAfterInput(
    current: List<KeywordPhrase>,
    typed: String,
    wholeWord: Boolean,
): List<KeywordPhrase> {
    val items = typed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val isList = items.size > 1
    return items.fold(current) { phrases, item ->
        val partOfWord = item.length > 2 && item.startsWith('*') && item.endsWith('*')
        when {
            partOfWord -> keywordPhrasesAfterAdd(phrases, item.substring(1, item.length - 1), wholeWord = false)
            isList -> keywordPhrasesAfterAdd(phrases, item, wholeWord = true)
            else -> keywordPhrasesAfterAdd(phrases, item, wholeWord)
        }
    }
}

/** One line to copy and paste back into the field: `raid, go live, *вайфер*`. */
internal fun keywordPhrasesExport(phrases: List<KeywordPhrase>): String {
    return phrases.joinToString(", ") { if (it.wholeWord) it.text else "*${it.text}*" }
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
    val array = raw.toJsonArrayOrNull() ?: return emptyList()
    val phrases = mutableListOf<KeywordPhrase>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index)
        val text = (item?.optString("text") ?: array.optString(index)).trim()
        // A saved duplicate keeps the first one's mode.
        if (text.isEmpty() || phrases.any { it.text.equals(text, ignoreCase = true) }) continue
        phrases += KeywordPhrase(text, item?.optBoolean("wholeWord") ?: true)
    }
    return phrases
}
