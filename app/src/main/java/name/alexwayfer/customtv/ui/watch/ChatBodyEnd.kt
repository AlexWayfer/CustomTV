package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatPart

/** The inline body without a trailing run that can wrap onto an empty line. GIFs are laid out below it. */
internal fun chatPartsForTextLayout(parts: List<ChatPart>): List<ChatPart> {
    val lastVisible = parts.indexOfLast { part ->
        when (part) {
            is ChatPart.Text -> part.text.any { !it.isInvisibleAtChatBodyEnd() }
            is ChatPart.Emote -> true
            is ChatPart.Gif -> false
        }
    }
    val inlineParts = if (lastVisible == parts.lastIndex) parts else parts.subList(0, lastVisible + 1)
    val lastText = inlineParts.lastOrNull() as? ChatPart.Text ?: return inlineParts
    val trimmed = lastText.text.trimEnd(Char::isInvisibleAtChatBodyEnd)
    return if (trimmed == lastText.text) inlineParts else parts.subList(0, lastVisible) + ChatPart.Text(trimmed)
}

// A trailing Combining Grapheme Joiner makes preceding spaces count toward wrapping.
private fun Char.isInvisibleAtChatBodyEnd(): Boolean = isWhitespace() || this == '\u034F'
