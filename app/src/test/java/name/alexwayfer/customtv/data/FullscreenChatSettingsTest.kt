package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenChatSettingsTest {
    @Test
    fun aStoredModeReadsBack() {
        FullscreenChatMode.entries.forEach { assertEquals(it, fullscreenChatModeFromStored(it.stored)) }
    }

    @Test
    fun noStoredModeOrAnUnknownOneHidesTheChat() {
        assertEquals(FullscreenChatMode.Hidden, fullscreenChatModeFromStored(null))
        assertEquals(FullscreenChatMode.Hidden, fullscreenChatModeFromStored("sideways"))
    }

    @Test
    fun aStoredSideReadsBack() {
        FullscreenChatSide.entries.forEach { assertEquals(it, fullscreenChatSideFromStored(it.stored)) }
    }

    @Test
    fun noStoredSideOrAnUnknownOnePutsTheChatOnTheRight() {
        assertEquals(FullscreenChatSide.Right, fullscreenChatSideFromStored(null))
        assertEquals(FullscreenChatSide.Right, fullscreenChatSideFromStored("top"))
    }
}
