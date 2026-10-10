package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchLayoutTest {
    @Test
    fun aCutoutOnOneSideInsetsBothSidesOfTheFullScreenPlayer() {
        assertEquals(120, fullscreenCutoutSidePx(leftPx = 120, rightPx = 0))
        assertEquals(120, fullscreenCutoutSidePx(leftPx = 0, rightPx = 120))
    }

    @Test
    fun noCutoutLeavesTheFullScreenPlayerAtTheEdges() {
        assertEquals(0, fullscreenCutoutSidePx(leftPx = 0, rightPx = 0))
    }

    @Test
    fun aShorterListScrollsByTheHeightItLost() {
        assertEquals(200, chatAnchorShift(oldHeight = 1000, oldTopInset = 0, newHeight = 800, newTopInset = 0))
    }

    @Test
    fun anExpandedNoticeOverTheListScrollsByTheHeightItCovers() {
        assertEquals(150, chatAnchorShift(oldHeight = 1000, oldTopInset = 100, newHeight = 1000, newTopInset = 250))
    }

    @Test
    fun aCollapsedNoticeScrollsBack() {
        assertEquals(-150, chatAnchorShift(oldHeight = 1000, oldTopInset = 250, newHeight = 1000, newTopInset = 100))
    }

    @Test
    fun aListThatGrowsAsMuchAsItsNoticesDoesNotScroll() {
        assertEquals(0, chatAnchorShift(oldHeight = 1000, oldTopInset = 100, newHeight = 1100, newTopInset = 200))
    }
}
