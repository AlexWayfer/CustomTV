package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSeekbarOverlayTest {
    @Test
    fun `the layer shows with the controls on a full player`() {
        assertTrue(shown())
    }

    @Test
    fun `the layer hides with the controls`() {
        assertFalse(shown(controlsVisible = false))
    }

    @Test
    fun `the layer waits for the video to load`() {
        assertFalse(shown(videoLoading = true))
    }

    @Test
    fun `the layer hides while Twitch's settings menu is open`() {
        assertFalse(shown(settingsMenuOpen = true))
    }

    @Test
    fun `the layer hides as the player starts to minimize`() {
        assertFalse(shown(progress = 0.01f))
    }

    private fun shown(
        controlsVisible: Boolean = true,
        videoLoading: Boolean = false,
        settingsMenuOpen: Boolean = false,
        progress: Float = 0f,
    ) = playerSeekbarOverlayShown(controlsVisible, videoLoading, settingsMenuOpen, progress)
}
