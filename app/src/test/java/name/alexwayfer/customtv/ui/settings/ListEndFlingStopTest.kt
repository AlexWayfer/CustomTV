package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListEndFlingStopTest {
    @Test
    fun `a frame toward an end the list is at stops the fling`() {
        assertTrue(listEndStopsFling(5f, canScrollForward = false, canScrollBackward = true))
        assertTrue(listEndStopsFling(-5f, canScrollForward = true, canScrollBackward = false))
    }

    @Test
    fun `a frame toward room left goes on`() {
        assertFalse(listEndStopsFling(5f, canScrollForward = true, canScrollBackward = false))
        assertFalse(listEndStopsFling(-5f, canScrollForward = false, canScrollBackward = true))
    }

    @Test
    fun `a frame that does not move goes on`() {
        assertFalse(listEndStopsFling(0f, canScrollForward = false, canScrollBackward = false))
    }
}
