package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentVisibilityTest {
    @Test
    fun anExpandedPlayerComposesItsContent() {
        assertTrue(playerContentComposed(inPictureInPicture = false, compactSettled = false, measuredFullSize = false))
    }

    @Test
    fun aMiniPlayerKeepsContentLaidOutAtFullSize() {
        assertTrue(playerContentComposed(inPictureInPicture = false, compactSettled = true, measuredFullSize = true))
    }

    @Test
    fun aPlayerOpenedMinimizedWaitsForItsFirstExpandToComposeContent() {
        assertFalse(playerContentComposed(inPictureInPicture = false, compactSettled = true, measuredFullSize = false))
    }

    @Test
    fun pictureInPictureDropsTheContent() {
        assertFalse(playerContentComposed(inPictureInPicture = true, compactSettled = false, measuredFullSize = true))
    }

    @Test
    fun aPlayerRestingAtFullSizeLeavesTouchesToItsContent() {
        assertTrue(playerContentTakesTouches(dragging = false, progress = 0f))
    }

    @Test
    fun aDraggedPlayerKeepsTouchesFromItsContent() {
        assertFalse(playerContentTakesTouches(dragging = true, progress = 0.3f))
        assertFalse(playerContentTakesTouches(dragging = true, progress = 0f))
    }

    @Test
    fun aPlayerSettlingAfterADragKeepsTouchesFromItsContent() {
        assertFalse(playerContentTakesTouches(dragging = false, progress = 0.3f))
    }
}
