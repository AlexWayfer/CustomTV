package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchAboutBrowserTest {
    @Test
    fun aChannelLoginProducesItsTwitchAboutPage() {
        assertEquals("https://www.twitch.tv/alex_wayfer/about", twitchAboutUrl("#Alex_Wayfer"))
    }

    @Test
    fun aThousandPixelContainerProducesAnEightHundredFiftyPixelSheet() {
        assertEquals(850, aboutBrowserSheetHeightPx(1_000))
    }
}
