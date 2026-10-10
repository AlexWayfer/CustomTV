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
            val pipe1 = gifsTag.indexOf('|', index)
            if (pipe1 < 0) break
            val range = gifsTag.substring(index, pipe1)
            val dash = range.indexOf('-')
            if (dash < 0) break
            val start = range.substring(0, dash).toIntOrNull() ?: break
            val end = range.substring(dash + 1).toIntOrNull() ?: break
            val pipe2 = gifsTag.indexOf('|', pipe1 + 1)
            if (pipe2 < 0) break
            val id = gifsTag.substring(pipe1 + 1, pipe2)
            val urlStart = pipe2 + 1
            val next = nextGifEntrySeparator(gifsTag, urlStart)
            val url = gifsTag.substring(urlStart, if (next < 0) gifsTag.length else next)
            if (url.isNotBlank()) {
                spans += ChatGifSpan(start, end, id, url)
            }
            index = if (next < 0) gifsTag.length else next + 1
        }
        return spans
    }

    fun parse(message: String, emotesTag: String, gifsTag: String = ""): List<ChatPart> {
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
        parseGifTag(gifsTag).forEach { gif ->
            ranges += EmoteRange(gif.start, gif.endInclusive, gifUrl = gif.url)
        }
        if (ranges.isEmpty()) return listOf(ChatPart.Text(message))
        ranges.sortBy { it.start }

        val codePoints = message.codePoints().toArray()
        val parts = mutableListOf<ChatPart>()
        var cursor = 0
        for ((start, endInclusive, emoteId, gifUrl) in ranges) {
            if (start < 0 || endInclusive >= codePoints.size || start > endInclusive) continue
            if (start < cursor) continue
            if (start > cursor) {
                parts += ChatPart.Text(String(codePoints, cursor, start - cursor))
            }
            val name = String(codePoints, start, endInclusive - start + 1)
            parts += gifUrl?.let { ChatPart.Gif(name, it) }
                ?: ChatPart.Emote(name, twitchEmoteUrl(emoteId.orEmpty()))
            cursor = endInclusive + 1
        }
        if (cursor < codePoints.size) {
            parts += ChatPart.Text(String(codePoints, cursor, codePoints.size - cursor))
        }
        return parts.ifEmpty { listOf(ChatPart.Text(message)) }
    }

    private fun nextGifEntrySeparator(tag: String, from: Int): Int {
        var search = from
        while (search < tag.length) {
            val comma = tag.indexOf(',', search)
            if (comma < 0) return -1
            if (isGifRangePrefix(tag, comma + 1)) return comma
            search = comma + 1
        }
        return -1
    }

    private fun isGifRangePrefix(tag: String, at: Int): Boolean {
        var index = at
        if (index >= tag.length || !tag[index].isDigit()) return false
        while (index < tag.length && tag[index].isDigit()) index++
        if (index >= tag.length || tag[index] != '-') return false
        index++
        if (index >= tag.length || !tag[index].isDigit()) return false
        while (index < tag.length && tag[index].isDigit()) index++
        return index < tag.length && tag[index] == '|'
    }

    private data class EmoteRange(
        val start: Int,
        val endInclusive: Int,
        val emoteId: String? = null,
        val gifUrl: String? = null,
    )
}
