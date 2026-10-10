package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayChatMorphTest {
    @Test
    fun theChatSinksToTheCompactRowsWithAGapUnderEach() {
        val rows = listOf<Pair<Any, Int>>("a" to 40, "b" to 60, "c" to 50)
        assertEquals(122, compactRowsHeightPx(rows, listOf("b", "c"), gapPx = 6))
    }

    @Test
    fun noCompactRowOnScreenLeavesTheTargetUnknown() {
        val rows = listOf<Pair<Any, Int>>("a" to 40, "b" to 60)
        assertNull(compactRowsHeightPx(rows, listOf("z"), gapPx = 6))
    }

    @Test
    fun anEmptyCompactChatTakesTheChatDownToNothing() {
        val rows = listOf<Pair<Any, Int>>("a" to 40, "b" to 60)
        assertEquals(0, compactRowsHeightPx(rows, emptyList(), gapPx = 6))
    }

    @Test
    fun aRowWithAKeyOtherThanAMessageIdDoesNotCount() {
        val rows = listOf<Pair<Any, Int>>(7 to 30, "b" to 60)
        assertEquals(66, compactRowsHeightPx(rows, listOf("7", "b"), gapPx = 6))
    }

    @Test
    fun aRowWithOneCompactRowUnderItRisesByOneGap() {
        assertEquals(6f, overlayChatRowLiftPx(gapsBelow = 1, gapPx = 6f, progress = 1f), 0f)
    }

    @Test
    fun aHigherRowRisesByEveryGapUnderIt() {
        assertEquals(18f, overlayChatRowLiftPx(gapsBelow = 3, gapPx = 6f, progress = 1f), 0f)
        assertEquals(9f, overlayChatRowLiftPx(gapsBelow = 3, gapPx = 6f, progress = 0.5f), 0.0001f)
    }

    @Test
    fun aRowStaysPutBeforeTheMorphAndWithoutGaps() {
        assertEquals(0f, overlayChatRowLiftPx(gapsBelow = 3, gapPx = 6f, progress = 0f), 0f)
        assertEquals(0f, overlayChatRowLiftPx(gapsBelow = -1, gapPx = 6f, progress = 1f), 0f)
        assertEquals(6f, overlayChatRowLiftPx(gapsBelow = 1, gapPx = 6f, progress = 1.5f), 0f)
    }

    @Test
    fun anOverlaidChatLooksCompactTheLessItIsExpanded() {
        assertEquals(0f, overlayChatMorphProgress(overlaid = true, expandProgress = 1f), 0f)
        assertEquals(0.25f, overlayChatMorphProgress(overlaid = true, expandProgress = 0.75f), 0.0001f)
        assertEquals(1f, overlayChatMorphProgress(overlaid = true, expandProgress = 0f), 0f)
    }

    @Test
    fun aChatBesideTheVideoNeverLooksCompact() {
        assertEquals(0f, overlayChatMorphProgress(overlaid = false, expandProgress = 0.3f), 0f)
    }

    @Test
    fun theMorphProgressStaysWithinRange() {
        assertEquals(1f, overlayChatMorphProgress(overlaid = true, expandProgress = -0.5f), 0f)
        assertEquals(0f, overlayChatMorphProgress(overlaid = true, expandProgress = 1.5f), 0f)
    }
}
