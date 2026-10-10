package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPictureScaleTest {
    @Test
    fun anExpandedPlayerIsNotScaled() {
        assertEquals(1f, playerPictureScale(boxWidthPx = 1080, fullWidthPx = 1080), 0f)
    }

    @Test
    fun aCollapsingPlayerShrinksToItsBoxWidth() {
        assertEquals(0.5f, playerPictureScale(boxWidthPx = 540, fullWidthPx = 1080), 0f)
    }

    @Test
    fun anUnmeasuredFullWidthLeavesThePictureUnscaled() {
        assertEquals(1f, playerPictureScale(boxWidthPx = 540, fullWidthPx = 0), 0f)
    }
}
