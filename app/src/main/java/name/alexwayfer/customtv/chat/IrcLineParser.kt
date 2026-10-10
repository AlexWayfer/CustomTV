package name.alexwayfer.customtv.chat

internal object IrcLineParser {
    fun parse(raw: String): IrcLine? {
        if (raw.isBlank()) return null
        var rest = raw
        val tags = linkedMapOf<String, String>()
        if (rest.startsWith('@')) {
            val space = rest.indexOf(' ')
            if (space < 0) return null
            rest.substring(1, space).split(';').forEach { token ->
                val equals = token.indexOf('=')
                if (equals < 0) {
                    tags[token] = ""
                } else {
                    tags[token.substring(0, equals)] = unescapeTag(token.substring(equals + 1))
                }
            }
            rest = rest.substring(space + 1)
        }

        var prefix: String? = null
        if (rest.startsWith(':')) {
            val space = rest.indexOf(' ')
            if (space < 0) return null
            prefix = rest.substring(0, space)
            rest = rest.substring(space + 1)
        }

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
