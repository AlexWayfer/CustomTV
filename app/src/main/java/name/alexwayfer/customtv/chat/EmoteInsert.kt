package name.alexwayfer.customtv.chat

internal data class EmoteInsertion(
    val text: String,
    val cursor: Int,
)

internal fun textWithInsertedEmote(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    emoteName: String,
    maxLength: Int,
): EmoteInsertion? {
    val name = emoteName.trim()
    if (name.isEmpty()) return null
    val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val before = text.substring(0, start)
    val after = text.substring(end)
    val leadingSpace = before.isNotEmpty() && !before.endsWith(" ")
    val withoutTrailingSpace = before.length + (if (leadingSpace) 1 else 0) + name.length + after.length
    // At the end of the text the space is left out only when it would pass the limit.
    val trailingSpace = !after.startsWith(" ") && (after.isNotEmpty() || withoutTrailingSpace < maxLength)
    val insert = buildString {
        if (leadingSpace) append(' ')
        append(name)
        if (trailingSpace) append(' ')
    }
    val next = before + insert + after
    if (next.length > maxLength) return null
    var cursor = before.length + (if (leadingSpace) 1 else 0) + name.length
    if (trailingSpace) cursor += 1
    else if (after.startsWith(" ")) cursor += 1
    return EmoteInsertion(next, cursor.coerceIn(0, next.length))
}

/**
 * The search row of the emote picker starts a `:` completion at the cursor, so the user learns that
 * typing a colon and a name finds an emote. A cursor already inside a `:` query keeps the text as is.
 * Returns null when the colon would pass the limit.
 */
internal fun textWithEmoteSearchColon(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    maxLength: Int,
): EmoteInsertion? {
    val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val before = text.substring(0, start)
    val after = text.substring(end)
    if (start == end && emoteQueryAtCursor(text, end) != null) return EmoteInsertion(text, end)
    val leadingSpace = before.isNotEmpty() && !before.last().isWhitespace()
    // Text right after the colon would join the query, so a space keeps it apart.
    val trailingSpace = after.isNotEmpty() && !after.first().isWhitespace()
    val insert = buildString {
        if (leadingSpace) append(' ')
        append(':')
        if (trailingSpace) append(' ')
    }
    val next = before + insert + after
    if (next.length > maxLength) return null
    return EmoteInsertion(next, before.length + (if (leadingSpace) 2 else 1))
}

/**
 * The keyboard height the emote picker takes. Only a keyboard that has finished rising counts:
 * the frames of its show and hide animations would leave a smaller height behind.
 */
internal fun settledKeyboardHeightPx(imeBottomPx: Int, imeTargetBottomPx: Int, rememberedPx: Int): Int =
    if (imeBottomPx > 0 && imeBottomPx == imeTargetBottomPx) imeBottomPx else rememberedPx

internal fun emotePanelHeightPx(
    navigationBarPx: Int,
    imeBottomPx: Int,
    pickerOpen: Boolean,
    keyboardReturning: Boolean,
    rememberedImePx: Int,
    fallbackPx: Int,
): Int {
    // While the keyboard rises in place of the closed picker, the picker's space stays reserved. The picker is as
    // tall as the keyboard it replaces, so the chat above them keeps its height; the fallback is only for a keyboard
    // not seen yet.
    val pickerHeightPx = if (rememberedImePx > 0) rememberedImePx else fallbackPx
    val pickerPx = if (pickerOpen || keyboardReturning) pickerHeightPx else 0
    return maxOf(navigationBarPx, imeBottomPx, pickerPx)
}
