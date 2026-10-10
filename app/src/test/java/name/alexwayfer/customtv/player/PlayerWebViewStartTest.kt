package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerWebViewStartTest {
    @Test
    fun theFirstDrawnFrameKeepsTheBlackPlaceholder() {
        assertFalse(playerWebViewStarts(framesDrawn = 0))
        assertFalse(playerWebViewStarts(framesDrawn = 1))
    }

    @Test
    fun theWebViewStartsAfterTheScreenHasBeenDrawn() {
        assertTrue(playerWebViewStarts(framesDrawn = 2))
    }
}
