package name.alexwayfer.customtv.chat

internal object IrcEmoteParser {
    fun shiftEmoteTag(emotesTag: String, shift: Int): String {
        if (shift <= 0 || emotesTag.isBlank()) return emotesTag
        return emotesTag.split('/').mapNotNull { entry ->
            if (entry.isBlank()) return@mapNotNull null
            val colon = entry.indexOf(':')
            if (colon < 0) return@mapNotNull null
            val id = entry.substring(0, colon)
            val spans = entry.substring(colon + 1).split(',').mapNotNull { span ->
                val dash = span.indexOf('-')
                if (dash < 0) return@mapNotNull null
                val start = (span.substring(0, dash).toIntOrNull() ?: return@mapNotNull null) - shift
                val end = (span.substring(dash + 1).toIntOrNull() ?: return@mapNotNull null) - shift
                if (end < 0) return@mapNotNull null
                "${start.coerceAtLeast(0)}-$end"
            }
            if (spans.isEmpty()) null else "$id:${spans.joinToString(",")}"
        }.joinToString("/")
    }

    fun shiftGifTag(gifsTag: String, shift: Int): String {
        if (shift <= 0 || gifsTag.isBlank()) return gifsTag
        return parseGifTag(gifsTag).mapNotNull { span ->
            val start = span.start - shift
            val end = span.endInclusive - shift
            if (end < 0) return@mapNotNull null
            "${start.coerceAtLeast(0)}-${end}|${span.id}|${span.url}"
        }.joinToString(",")
    }

    fun parseGifTag(gifsTag: String): List<ChatGifSpan> {
        if (gifsTag.isBlank()) return emptyList()
        val spans = mutableListOf<ChatGifSpan>()
        var index = 0
        while (index < gifsTag.length) {
            val entry = gifEntryAt(gifsTag, index) ?: break
            entry.span?.let { spans += it }
            index = entry.next
        }
        return spans
    }

    /** One `<start>-<end>|<id>|<url>` entry; its span is null for a blank URL. */
    private class GifEntry(val span: ChatGifSpan?, val next: Int)

    /** The entry at [index] of the tag, or null when it is broken and the rest cannot be read. */
    private fun gifEntryAt(tag: String, index: Int): GifEntry? {
        val pipe1 = tag.indexOf('|', index).takeIf { it >= 0 } ?: return null
        val dash = tag.indexOf('-', index).takeIf { it in index until pipe1 } ?: return null
        val start = tag.substring(index, dash).toIntOrNull() ?: return null
        val end = tag.substring(dash + 1, pipe1).toIntOrNull() ?: return null
        val pipe2 = tag.indexOf('|', pipe1 + 1).takeIf { it >= 0 } ?: return null
        val id = tag.substring(pipe1 + 1, pipe2)
        val urlStart = pipe2 + 1
        val next = nextGifEntrySeparator(tag, urlStart)
        val url = tag.substring(urlStart, if (next < 0) tag.length else next)
        return GifEntry(
            span = ChatGifSpan(start, end, id, url).takeIf { url.isNotBlank() },
            next = if (next < 0) tag.length else next + 1,
        )
    }

    fun parse(message: String, emotesTag: String, gifsTag: String = ""): List<ChatPart> {
        val ranges = emoteRanges(emotesTag)
        parseGifTag(gifsTag).forEach { gif ->
            ranges += EmoteRange(gif.start, gif.endInclusive, gifUrl = gif.url)
        }
        if (ranges.isEmpty()) return listOf(ChatPart.Text(message))
        ranges.sortBy { it.start }

        val codePoints = message.codePoints().toArray()
        val parts = mutableListOf<ChatPart>()
        var cursor = 0
        for (range in ranges) {
            val (start, endInclusive) = range
            // The cursor is never below zero, so this also skips a negative start.
            if (start !in cursor..endInclusive || endInclusive >= codePoints.size) continue
            if (start > cursor) {
                parts += ChatPart.Text(String(codePoints, cursor, start - cursor))
            }
            val name = String(codePoints, start, endInclusive - start + 1)
            parts += range.gifUrl?.let { ChatPart.Gif(name, it) }
                ?: ChatPart.Emote(name, twitchEmoteUrl(range.emoteId.orEmpty()))
            cursor = endInclusive + 1
        }
        if (cursor < codePoints.size) {
            parts += ChatPart.Text(String(codePoints, cursor, codePoints.size - cursor))
        }
        return parts.ifEmpty { listOf(ChatPart.Text(message)) }
    }

    /** The emote positions of the `emotes` tag, `id:start-end,start-end/id:...`; a broken entry is skipped. */
    private fun emoteRanges(emotesTag: String): MutableList<EmoteRange> {
        val ranges = mutableListOf<EmoteRange>()
        emotesTag.split('/').forEach { entry ->
            if (entry.isBlank()) return@forEach
            val colon = entry.indexOf(':')
            if (colon < 0) return@forEach
            val id = entry.substring(0, colon)
            entry.substring(colon + 1).split(',').forEach { span ->
                val dash = span.indexOf('-')
                if (dash < 0) return@forEach
                val start = span.substring(0, dash).toIntOrNull() ?: return@forEach
                val end = span.substring(dash + 1).toIntOrNull() ?: return@forEach
                ranges += EmoteRange(start, end, emoteId = id)
            }
        }
        return ranges
    }

    private fun nextGifEntrySeparator(tag: String, from: Int): Int {
        var comma = tag.indexOf(',', from)
        while (comma >= 0 && !isGifRangePrefix(tag, comma + 1)) comma = tag.indexOf(',', comma + 1)
        return comma
    }

    /** Whether [tag] has `<digits>-<digits>|` at [at], the start of the next GIF entry. */
    private fun isGifRangePrefix(tag: String, at: Int): Boolean {
        val dash = digitsEnd(tag, at)
        if (dash == at || dash >= tag.length || tag[dash] != '-') return false
        val bar = digitsEnd(tag, dash + 1)
        return bar > dash + 1 && bar < tag.length && tag[bar] == '|'
    }

    private fun digitsEnd(tag: String, from: Int): Int {
        var index = from
        while (index < tag.length && tag[index].isDigit()) index++
        return index
    }

    private data class EmoteRange(
        val start: Int,
        val endInclusive: Int,
        val emoteId: String? = null,
        val gifUrl: String? = null,
    )
}
