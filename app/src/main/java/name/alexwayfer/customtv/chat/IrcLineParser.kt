package name.alexwayfer.customtv.chat

internal object IrcLineParser {
    fun parse(raw: String): IrcLine? {
        if (raw.isBlank()) return null
        val tagged = leadingWord(raw, '@') ?: return null
        val prefixed = leadingWord(tagged.rest, ':') ?: return null
        val tags = parseTags(tagged.word?.substring(1))
        val prefix = prefixed.word
        val rest = prefixed.rest

        val trailingIndex = rest.indexOf(" :")
        val trailing = if (trailingIndex >= 0) rest.substring(trailingIndex + 2) else null
        val middle = if (trailingIndex >= 0) rest.substring(0, trailingIndex) else rest
        val parts = middle.split(' ').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return IrcLine(
            tags = tags,
            prefix = prefix,
            command = parts.first(),
            params = parts.drop(1),
            trailing = trailing,
        )
    }

    private class LeadingWord(val word: String?, val rest: String)

    /**
     * The word that opens [line] with [marker], with the marker, and the text after its space. A line that does
     * not start with [marker] has no such word; one that is only that word is broken.
     */
    private fun leadingWord(line: String, marker: Char): LeadingWord? {
        if (!line.startsWith(marker)) return LeadingWord(null, line)
        val space = line.indexOf(' ')
        return if (space < 0) null else LeadingWord(line.substring(0, space), line.substring(space + 1))
    }

    private fun parseTags(tags: String?): Map<String, String> {
        val parsed = linkedMapOf<String, String>()
        tags?.split(';')?.forEach { token ->
            val equals = token.indexOf('=')
            if (equals < 0) {
                parsed[token] = ""
            } else {
                parsed[token.substring(0, equals)] = unescapeTag(token.substring(equals + 1))
            }
        }
        return parsed
    }

    private fun unescapeTag(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    ':' -> append(';')
                    's' -> append(' ')
                    '\\' -> append('\\')
                    'r' -> append('\r')
                    'n' -> append('\n')
                    else -> append(value[index + 1])
                }
                index += 2
            } else {
                append(character)
                index++
            }
        }
    }
}

data class IrcLine(
    val tags: Map<String, String>,
    val prefix: String?,
    val command: String,
    val params: List<String>,
    val trailing: String?,
)
