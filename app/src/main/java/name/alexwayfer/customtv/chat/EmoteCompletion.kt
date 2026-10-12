package name.alexwayfer.customtv.chat

internal fun emoteQueryAtCursor(
    text: String,
    cursor: Int,
    withoutColon: Boolean = false,
): EmoteQuery? =
    colonEmoteQuery(text, cursor) ?: if (withoutColon) wordEmoteQuery(text, cursor) else null

private fun colonEmoteQuery(text: String, cursor: Int): EmoteQuery? {
    if (cursor !in 1..text.length) return null
    if (text[cursor - 1].isWhitespace()) return null
    val tokenStart = tokenStartBefore(text, cursor)
    if (text[tokenStart] != ':') return null
    val tokenEnd = tokenEndAfter(text, cursor)
    return EmoteQuery(
        start = tokenStart,
        end = tokenEnd,
        text = text.substring(tokenStart + 1, tokenEnd),
    )
}

private fun wordEmoteQuery(text: String, cursor: Int): EmoteQuery? {
    if (cursor !in 1..text.length) return null
    if (text[cursor - 1].isWhitespace()) return null
    val tokenStart = tokenStartBefore(text, cursor)
    val tokenEnd = tokenEndAfter(text, cursor)
    // A message that starts with `/` is a command: its first word is the command, not an emote.
    val command = tokenStart == 0 && text[0] == '/'
    if (text[tokenStart] == ':' || command || tokenEnd == tokenStart) return null
    return EmoteQuery(
        start = tokenStart,
        end = tokenEnd,
        text = text.substring(tokenStart, tokenEnd),
    )
}

/** Where the word that ends at [cursor] starts. */
private fun tokenStartBefore(text: String, cursor: Int): Int {
    var start = cursor
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    return start
}

/** Where the word that runs through [cursor] ends. */
private fun tokenEndAfter(text: String, cursor: Int): Int {
    var end = cursor
    while (end < text.length && !text[end].isWhitespace()) end++
    return end
}

internal fun pickerEmotesForCompletion(sections: List<EmotePickerSection>): List<PickerEmote> {
    val seen = HashSet<String>()
    val result = ArrayList<PickerEmote>()
    for (section in sections) {
        for (emote in section.emotes) {
            if (seen.add(emote.name.lowercase())) result.add(emote)
        }
    }
    return result
}

internal fun matchingPickerEmotes(
    emotes: List<PickerEmote>,
    query: String,
    usage: List<EmoteUse> = emptyList(),
): List<PickerEmote> {
    // A colon alone offers the emotes of the picker's Frequently used section.
    if (query.isEmpty()) return frequentlyUsedEmotes(emotes, usage)
    val byName = usage.associateBy { it.name }
    return emotes
        .filter { emoteNameMatchIndex(it.name, query) >= 0 }
        .distinctBy { it.name.lowercase() }
        .sortedWith(
            // Used emotes come first, then the rest. Inside each group, names that start with the query lead.
            compareBy<PickerEmote> { byName[it.name] == null }
                .thenBy { emoteNameMatchIndex(it.name, query) != 0 }
                .thenByDescending { byName[it.name]?.count ?: 0 }
                .thenByDescending { byName[it.name]?.usedAtMillis ?: 0L }
                .thenBy { it.name.lowercase() },
        )
}

internal fun emoteNameMatchIndex(name: String, query: String): Int {
    if (query.isEmpty()) return -1
    val lower = name.lowercase()
    val needle = query.lowercase()
    val direct = lower.indexOf(needle)
    val colon = lower.indexOf(":$needle")
    return when {
        direct < 0 -> colon
        colon < 0 -> direct
        else -> minOf(direct, colon)
    }
}

internal fun textWithCompletedEmote(
    text: String,
    cursor: Int,
    emoteName: String,
    maxLength: Int,
    withoutColon: Boolean = false,
): EmoteInsertion? {
    val (start, end) = emoteQueryAtCursor(text, cursor, withoutColon) ?: return null
    val name = emoteName.trim()
    if (name.isEmpty()) return null
    val alreadySpaced = end < text.length && text[end] == ' '
    val insert = if (alreadySpaced) name else "$name "
    val next = text.substring(0, start) + insert + text.substring(end)
    if (next.length > maxLength) return null
    return EmoteInsertion(text = next, cursor = start + name.length + 1)
}
