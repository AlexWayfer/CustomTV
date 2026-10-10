package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileStickyHeaderTest {
    @Test
    fun anUnscrolledHeaderIsOpen() {
        assertEquals(0f, profileHeaderCollapse(scrollPx = 0, headerTopPx = 180, headerHeightPx = 300, barPx = 160), 0f)
    }

    @Test
    fun theHeaderIsHalfFoldedHalfwayToTheBar() {
        assertEquals(0.5f, profileHeaderCollapse(scrollPx = 160, headerTopPx = 180, headerHeightPx = 300, barPx = 160), 0f)
    }

    @Test
    fun theHeaderIsABarOnceItsRoomHasGoneUnderTheBar() {
        assertEquals(1f, profileHeaderCollapse(scrollPx = 900, headerTopPx = 180, headerHeightPx = 300, barPx = 160), 0f)
    }

    @Test
    fun aHeaderNoTallerThanTheBarFoldsAtOnce() {
        assertEquals(0f, profileHeaderCollapse(scrollPx = 0, headerTopPx = 0, headerHeightPx = 100, barPx = 160), 0f)
        assertEquals(1f, profileHeaderCollapse(scrollPx = 1, headerTopPx = 0, headerHeightPx = 100, barPx = 160), 0f)
    }

    @Test
    fun theLinesUnderTheNameAreGoneHalfwayThroughTheFold() {
        assertEquals(1f, profileHeaderDetailsAlpha(0f), 0f)
        assertEquals(0.5f, profileHeaderDetailsAlpha(0.25f), 0f)
        assertEquals(0f, profileHeaderDetailsAlpha(0.5f), 0f)
        assertEquals(0f, profileHeaderDetailsAlpha(1f), 0f)
    }

    @Test
    fun theTabRowScrollsWithTheProfileUntilItReachesTheBar() {
        assertEquals(0, profileTabRowPinPx(scrollPx = 100, barPx = 160, tabRowTopPx = 700))
    }

    @Test
    fun theTabRowStaysUnderTheBarOnceItGetsThere() {
        assertEquals(60, profileTabRowPinPx(scrollPx = 600, barPx = 160, tabRowTopPx = 700))
    }
}
