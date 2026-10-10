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
}
