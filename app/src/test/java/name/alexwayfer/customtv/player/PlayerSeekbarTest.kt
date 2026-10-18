package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSeekbarTest {
    @Test
    fun `a seek bar inside the page is kept`() {
        assertEquals(
            PlayerSeekbar(left = 0.04f, right = 0.96f, barTop = 0.76f, touchTop = 0.72f),
            playerSeekbarOrNull(left = 0.04, right = 0.96, barTop = 0.76, touchTop = 0.72),
        )
    }

    @Test
    fun `a share outside the page is dropped`() {
        assertNull(playerSeekbarOrNull(left = -0.1, right = 0.96, barTop = 0.76, touchTop = 0.72))
        assertNull(playerSeekbarOrNull(left = 0.04, right = 1.2, barTop = 0.76, touchTop = 0.72))
        assertNull(playerSeekbarOrNull(left = 0.04, right = 0.96, barTop = Double.NaN, touchTop = 0.72))
    }

    @Test
    fun `edges in the wrong order are dropped`() {
        assertNull(playerSeekbarOrNull(left = 0.5, right = 0.5, barTop = 0.76, touchTop = 0.72))
        assertNull(playerSeekbarOrNull(left = 0.04, right = 0.96, barTop = 0.70, touchTop = 0.72))
    }
}
