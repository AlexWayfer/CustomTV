package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControlBarTest {
    @Test
    fun `the bar share of the page scales to the web view height`() {
        assertEquals(90, controlBarHeightPx(0.1, 900))
        assertEquals(68, controlBarHeightPx(0.0753, 900))
    }

    @Test
    fun `a share outside the page stays within the web view`() {
        assertEquals(900, controlBarHeightPx(1.5, 900))
        assertEquals(0, controlBarHeightPx(-0.2, 900))
    }

    @Test
    fun `a touch on the shown controls, such as the seek bar, stays with the page`() {
        assertTrue(touchStaysWithControls(controlsShownAtDown = true, touchOnControls = true))
    }

    @Test
    fun `a touch on the video or on hidden controls can start a swipe`() {
        assertFalse(touchStaysWithControls(controlsShownAtDown = true, touchOnControls = false))
        assertFalse(touchStaysWithControls(controlsShownAtDown = false, touchOnControls = true))
    }

    @Test
    fun `an unmeasured web view gives no height`() {
        assertEquals(0, controlBarHeightPx(0.1, 0))
    }
}
