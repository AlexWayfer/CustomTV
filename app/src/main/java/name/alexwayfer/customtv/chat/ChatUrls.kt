package name.alexwayfer.customtv.chat

fun findChatUrls(text: String): List<ChatUrlSpan> {
    if (text.isEmpty()) return emptyList()
    val spans = mutableListOf<ChatUrlSpan>()
    for (match in CHAT_URL_REGEX.findAll(text)) {
        val raw = trimTrailingUrlPunctuation(match.value)
        // An empty link has no address either.
        val href = normalizeChatUrl(raw) ?: continue
        val start = match.range.first
        spans += ChatUrlSpan(
            start = start,
            endExclusive = start + raw.length,
            raw = raw,
            href = href,
        )
    }
    return spans
}

internal fun normalizeChatUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val href = when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
        isBareDomain(trimmed.substringBefore('/').substringBefore('?').substringBefore('#')) ->
            "https://$trimmed"
        else -> return null
    }
    val host = href.substringAfter("://", missingDelimiterValue = "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBefore(':')
    if (host.isEmpty() || '.' !in host || host.any { it.isWhitespace() }) return null
    return href
}

private fun isBareDomain(host: String): Boolean {
    if (!BARE_DOMAIN_REGEX.matches(host)) return false
    val tld = host.substringBefore(':').substringAfterLast('.').lowercase()
    return tld !in FILE_EXTENSIONS
}

private fun trimTrailingUrlPunctuation(raw: String): String {
    var end = raw.length
    while (end > 0 && trailsUrl(raw, end)) end--
    return raw.substring(0, end)
}

/** Whether the character before [end] closes the sentence around a link rather than belonging to it. */
private fun trailsUrl(raw: String, end: Int): Boolean = when (raw[end - 1]) {
    '.', ',', ';', ':', '!', '?' -> true
    ')' -> unbalanced(raw, end, '(', ')')
    ']' -> unbalanced(raw, end, '[', ']')
    else -> false
}

/** Whether the first [end] characters of [raw] close more brackets than they open. */
private fun unbalanced(raw: String, end: Int, open: Char, close: Char): Boolean {
    val slice = raw.substring(0, end)
    return slice.count { it == open } < slice.count { it == close }
}

private val CHAT_URL_REGEX = Regex(
    // A link with a scheme or `www.`, which may follow an `@` ("@https://..."), or a bare domain
    // with an optional port and path, which may not, so an email stays plain text.
    """(?i)(?<![\p{L}\p{N}_])(?:https?://|www\.)[^\s<>\[\]"']+|""" +
        """(?<![\p{L}\p{N}_@])(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,63}(?::[0-9]{1,5})?""" +
        """(?:[/?#][^\s<>\[\]"']*)?""",
)

private val BARE_DOMAIN_REGEX = Regex(
    """(?i)(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,63}(?::[0-9]{1,5})?""",
)

// File extensions that are not top-level domains, so "Document.txt" stays plain text.
// Extensions that are real TLDs (.md, .py, .sh, .zip, .mov) still link.
private val FILE_EXTENSIONS = setOf(
    "txt", "log", "ini", "cfg", "csv", "json", "xml", "yml", "yaml",
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg",
    "wav", "flac", "ogg", "mkv", "avi",
    "exe", "msi", "dll", "bat", "apk", "jar", "dmg", "iso", "rar", "tar", "bak", "tmp",
    "html", "htm", "css", "php", "java", "kts",
)
