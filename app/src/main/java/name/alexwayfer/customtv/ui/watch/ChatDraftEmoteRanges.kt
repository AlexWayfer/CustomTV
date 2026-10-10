package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.TextRange
import name.alexwayfer.customtv.chat.EmoteEffects

/**
 * Whether chat shows [token] as an emote or applies it to one: an emote the user can send, or,
 * with BTTV or FFZ on, one of their modifiers such as `w!` or `ffzW`. Emote codes are case-sensitive.
 */
internal fun chatDraftIsEmoteCode(
    token: String,
    emoteNames: Set<String>,
    bttvModifiers: Boolean,
    ffzModifiers: Boolean,
): Boolean = token in emoteNames ||
    (bttvModifiers && EmoteEffects.bttv(token) != null) ||
    (ffzModifiers && EmoteEffects.knownFfz(token) != null)

/**
 * The whitespace characters right before or after an emote range, in order, each with the number
 * of plaques it touches: 2 for a space between two emotes. Widening them by that many steps
 * keeps a visible gap next to every plaque.
 */
internal fun chatDraftEmoteGaps(text: CharSequence, ranges: List<TextRange>): Map<Int, Int> {
    val gaps = sortedMapOf<Int, Int>()
    for (range in ranges) {
        if (range.start > 0 && text[range.start - 1].isWhitespace()) gaps.merge(range.start - 1, 1, Int::plus)
        if (range.end < text.length && text[range.end].isWhitespace()) gaps.merge(range.end, 1, Int::plus)
    }
    return gaps
}

/**
 * Steps of letter spacing, one plaque padding each, for a gap that touches [plaques] plaques.
 * Every touching plaque covers one step of the space, and one more step keeps the gap wider than a
 * plain space, so a gap between two emotes looks as wide as a gap between an emote and a word.
 * Since Android 15 the line's last character keeps only the half of its letter spacing that lies
 * before it, so a space typed after an emote at the end of the draft gets twice as much.
 */
internal fun chatDraftEmoteGapSteps(plaques: Int, lastCharacter: Boolean): Int =
    (plaques + 1) * if (lastCharacter) 2 else 1

/** Steps of letter spacing on the gap right before [range], or 0 without one. */
internal fun chatDraftEmoteGapStepsBefore(range: TextRange, gaps: Map<Int, Int>): Int =
    gaps[range.start - 1]?.let { plaques -> chatDraftEmoteGapSteps(plaques, lastCharacter = false) } ?: 0

/**
 * Ranges of [text] to highlight as emotes: whole words, split on whitespace, that [isEmoteCode]
 * accepts. A code inside a longer word stays text.
 */
internal fun chatDraftEmoteRanges(text: CharSequence, isEmoteCode: (String) -> Boolean): List<TextRange> {
    val ranges = ArrayList<TextRange>()
    var start = 0
    while (start < text.length) {
        if (text[start].isWhitespace()) {
            start++
            continue
        }
        var end = start
        while (end < text.length && !text[end].isWhitespace()) end++
        if (isEmoteCode(text.substring(start, end))) ranges.add(TextRange(start, end))
        start = end
    }
    return ranges
}
