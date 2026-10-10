package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.FullscreenChatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenChatModeButtonTest {
    @Test
    fun anOverlaidChatMovesBesideTheVideo() {
        assertEquals(FullscreenChatMode.Column, fullscreenChatNextMode(FullscreenChatMode.Overlay))
    }

    @Test
    fun aColumnChatTurnsOff() {
        assertEquals(FullscreenChatMode.Hidden, fullscreenChatNextMode(FullscreenChatMode.Column))
    }

    @Test
    fun aHiddenChatComesBackOverTheVideo() {
        assertEquals(FullscreenChatMode.Overlay, fullscreenChatNextMode(FullscreenChatMode.Hidden))
    }
}
