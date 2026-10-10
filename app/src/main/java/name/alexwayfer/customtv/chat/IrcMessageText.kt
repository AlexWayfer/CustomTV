package name.alexwayfer.customtv.chat

internal object IrcMessageText {
    fun stripCtcpAction(text: String): CtcpActionStrip {
        if (!text.startsWith(CTCP_ACTION_PREFIX)) {
            return CtcpActionStrip(text = text, isAction = false, prefixCodePoints = 0)
        }
        val afterCommand = CTCP_ACTION_PREFIX.length
        if (afterCommand < text.length && text[afterCommand] != ' ' && text[afterCommand] != CTCP_SOH) {
            return CtcpActionStrip(text = text, isAction = false, prefixCodePoints = 0)
        }
        var start = afterCommand
        if (start < text.length && text[start] == ' ') start++
        var end = text.length
        if (end > start && text[end - 1] == CTCP_SOH) end--
        return CtcpActionStrip(
            text = text.substring(start, end),
            isAction = true,
            prefixCodePoints = text.codePointCount(0, start),
        )
    }

    fun stripLeadingReplyMention(text: String, reply: ChatReply): String {
        val names = listOf(reply.parentDisplayName, reply.parentUserLogin)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        for (name in names) {
            val prefix = "@$name"
            if (!text.startsWith(prefix, ignoreCase = true)) continue
            val after = prefix.length
            if (after < text.length && !text[after].isWhitespace()) continue
            var end = after
            while (end < text.length && text[end].isWhitespace()) end++
            return text.substring(end)
        }
        return text
    }

    private const val CTCP_SOH = '\u0001'
    private const val CTCP_ACTION_PREFIX = "\u0001ACTION"
}

internal data class CtcpActionStrip(
    val text: String,
    val isAction: Boolean,
    val prefixCodePoints: Int,
)
