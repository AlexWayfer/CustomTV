package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchChatBrowserTest {
    @Test
    fun chatUrlUsesTheChannelPopoutPage() {
        assertEquals(
            "https://www.twitch.tv/popout/example_channel/chat?popout=",
            twitchChatUrl("example_channel"),
        )
    }

    @Test
    fun browserSheetFillsTheAreaBelowThePlayer() {
        assertEquals(1_400, chatBrowserSheetHeightPx(containerHeightPx = 2_400, playerBottomPx = 1_000))
        assertEquals(0, chatBrowserSheetHeightPx(containerHeightPx = 1_000, playerBottomPx = 1_200))
    }
}
