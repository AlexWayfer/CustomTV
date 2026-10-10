package name.alexwayfer.customtv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeAwayTest {
    @Test
    fun aSwipePastAThirdOfTheWidthDismissesEitherWay() {
        assertTrue(swipeAwayDismisses(offsetX = 101f, velocityX = 0f, widthPx = 300))
        assertTrue(swipeAwayDismisses(offsetX = -101f, velocityX = 0f, widthPx = 300))
    }

    @Test
    fun aShortSlowSwipeSpringsBack() {
        assertFalse(swipeAwayDismisses(offsetX = 99f, velocityX = 0f, widthPx = 300))
    }

    @Test
    fun aShortFlingTheWayItMovedDismisses() {
        assertTrue(swipeAwayDismisses(offsetX = 20f, velocityX = 4000f, widthPx = 300))
    }

    @Test
    fun aFlingBackTowardItsPlaceSpringsBack() {
        assertFalse(swipeAwayDismisses(offsetX = 20f, velocityX = -4000f, widthPx = 300))
    }

    @Test
    fun noMoveNeverDismisses() {
        assertFalse(swipeAwayDismisses(offsetX = 0f, velocityX = 4000f, widthPx = 300))
    }

    @Test
    fun theViewFadesOutAtItsWidthAndStaysOpaqueUnmeasured() {
        assertEquals(0.5f, swipeAwayAlpha(offsetX = -150f, widthPx = 300), 0.001f)
        assertEquals(0f, swipeAwayAlpha(offsetX = 400f, widthPx = 300), 0.001f)
        assertEquals(1f, swipeAwayAlpha(offsetX = 50f, widthPx = 0), 0.001f)
    }
}
