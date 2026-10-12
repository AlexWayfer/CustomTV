package name.alexwayfer.customtv.chat

fun findChatNicks(text: String, knownKeys: Set<String>): List<ChatNickSpan> {
    if (text.isEmpty()) return emptyList()
    val urls = findChatUrls(text)
    val mentions = mutableListOf<ChatNickSpan>()
    for (match in MENTION_REGEX.findAll(text)) {
        val login = match.groupValues[1]
        val start = match.range.first
        val end = match.range.last + 1
        if (isUrlLikeMention(login, text, end) || overlapsUrl(start, end, urls)) continue
        mentions += ChatNickSpan(
            start = start,
            endExclusive = end,
            raw = match.value,
            key = login.lowercase(),
            mentioned = true,
        )
    }
    if (knownKeys.isEmpty()) return mentions
    val nicks = mentions.toMutableList()
    for (match in BARE_NICK_REGEX.findAll(text)) {
        val login = match.value
        val key = login.lowercase()
        val start = match.range.first
        val end = match.range.last + 1
        if (key !in knownKeys || overlapsUrl(start, end, urls) || overlapsNick(start, end, nicks)) continue
        nicks += ChatNickSpan(
            start = start,
            endExclusive = end,
            raw = login,
            key = key,
            mentioned = false,
        )
    }
    nicks.sortBy { it.start }
    return nicks
}

private fun isUrlLikeMention(login: String, text: String, end: Int): Boolean {
    val rest = if (end < text.length) text.substring(end) else ""
    if (rest.startsWith("://")) return true
    if (login.lowercase() in URL_SCHEMES && (rest.startsWith(":") || rest.startsWith("."))) {
        return true
    }
    return false
}

private fun overlapsUrl(start: Int, end: Int, urls: List<ChatUrlSpan>): Boolean {
    return urls.any { it.start < end && it.endExclusive > start }
}

private fun overlapsNick(start: Int, end: Int, nicks: List<ChatNickSpan>): Boolean =
    nicks.any { it.start < end && it.endExclusive > start }

private val MENTION_REGEX = Regex("""(?<![A-Za-z0-9_])@([A-Za-z0-9_]{4,25})(?![A-Za-z0-9_])""")
private val BARE_NICK_REGEX = Regex("""(?<![A-Za-z0-9_@])([A-Za-z0-9_]{4,25})(?![A-Za-z0-9_])""")
private val URL_SCHEMES = setOf("http", "https", "ftp", "ftps", "www")
