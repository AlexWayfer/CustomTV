package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVerticalDragTest {
    @Test
    fun downwardSwipeOnScrollableSettingsDoesNotMinimize() {
        assertFalse(shouldStartPlayerVerticalDrag(40f, 0f, 8, canMinimize = true, canExpand = false, touchOnScrollableContent = true))
    }

    @Test
    fun downwardSwipeOnVideoStillMinimizes() {
        assertTrue(shouldStartPlayerVerticalDrag(40f, 0f, 8, canMinimize = true, canExpand = false, touchOnScrollableContent = false))
    }

    @Test
    fun upwardSwipeOnVideoStillExpands() {
        assertTrue(shouldStartPlayerVerticalDrag(-40f, 0f, 8, canMinimize = false, canExpand = true, touchOnScrollableContent = false))
    }

    @Test
    fun aSwipeFromTheStatusBarBandIsLeftToTheSystem() {
        assertFalse(
            shouldStartPlayerVerticalDrag(
                40f,
                0f,
                8,
                canMinimize = true,
                canExpand = false,
                touchOnScrollableContent = false,
                startsInStatusBarZone = true,
            ),
        )
    }

    @Test
    fun aTouchAboveTheStatusBarHeightStartsInItsBand() {
        assertTrue(touchStartsInStatusBarZone(downRawY = 30f, statusBarPx = 80))
    }

    @Test
    fun aTouchBelowTheStatusBarHeightStartsOutsideItsBand() {
        assertFalse(touchStartsInStatusBarZone(downRawY = 80f, statusBarPx = 80))
        assertFalse(touchStartsInStatusBarZone(downRawY = 10f, statusBarPx = 0))
    }

    @Test
    fun aDownwardDragFromTheFullPlayerHidesTheKeyboard() {
        assertTrue(minimizeDragHidesKeyboard(fromMiniPlayer = false, offsetY = 40f))
        assertFalse(minimizeDragHidesKeyboard(fromMiniPlayer = true, offsetY = -40f))
        assertFalse(minimizeDragHidesKeyboard(fromMiniPlayer = false, offsetY = 0f))
    }

    @Test
    fun aMinimizeDragThatClosesTheKeyboardKeepsTheNavigationBar() {
        assertTrue(minimizeDragKeepsNavigationBar(fromMiniPlayer = false, offsetY = 40f, imeBottomPx = 800))
    }

    @Test
    fun aMinimizeDragWithoutTheKeyboardDoesNotKeepTheNavigationBar() {
        assertFalse(minimizeDragKeepsNavigationBar(fromMiniPlayer = false, offsetY = 40f, imeBottomPx = 0))
    }

    @Test
    fun anExpandDragFromTheMiniPlayerDoesNotKeepTheNavigationBar() {
        assertFalse(minimizeDragKeepsNavigationBar(fromMiniPlayer = true, offsetY = -40f, imeBottomPx = 800))
    }
}
