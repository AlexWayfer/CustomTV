package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class FlaggedTermRangesTest {
    @Test
    fun findsEveryOccurrenceIgnoringCaseAfterTheAuthor() {
        val text = "Streamrise: hi streamrise and STREAMRISE"
        assertEquals(listOf(15..24, 30..39), flaggedTermRanges(text, listOf("streamrise"), from = 12))
    }

    @Test
    fun longerTermWinsWhereTwoOverlap() {
        assertEquals(listOf(0..8), flaggedTermRanges("bad words", listOf("bad", "bad words")))
    }

    @Test
    fun blankTermsAndNoMatchGiveNoRanges() {
        assertEquals(emptyList<IntRange>(), flaggedTermRanges("text", listOf(" ", "other")))
    }
}
