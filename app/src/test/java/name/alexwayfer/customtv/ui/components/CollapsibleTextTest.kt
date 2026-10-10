package name.alexwayfer.customtv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CollapsibleTextTest {
    @Test
    fun anOpenOrMovingTextLaysOutEveryLine() = assertEquals(Int.MAX_VALUE, collapsibleTextMaxLines(allLines = true))

    @Test
    fun aCollapsedTextKeepsToOneLine() = assertEquals(1, collapsibleTextMaxLines(allLines = false))

    @Test
    fun aCollapsedTextShowsOnlyTheFirstLine() = assertEquals(16, revealedLinesHeightPx(16, 48, progress = 0f))

    @Test
    fun anOpenTextShowsEveryLine() = assertEquals(48, revealedLinesHeightPx(16, 48, progress = 1f))

    @Test
    fun halfwayTheLinesBelowTheFirstAreHalfShown() = assertEquals(32, revealedLinesHeightPx(16, 48, progress = 0.5f))

    @Test
    fun aOneLineTextKeepsItsHeightThroughout() = assertEquals(16, revealedLinesHeightPx(16, 16, progress = 0.3f))

    @Test
    fun anUnmeasuredOrEmptyTextNeverGoesBelowZeroOrPastItsHeight() {
        assertEquals(0, revealedLinesHeightPx(0, 0, progress = 0.5f))
        assertEquals(10, revealedLinesHeightPx(20, 10, progress = 0f))
        assertEquals(48, revealedLinesHeightPx(16, 48, progress = 1.4f))
    }
}
