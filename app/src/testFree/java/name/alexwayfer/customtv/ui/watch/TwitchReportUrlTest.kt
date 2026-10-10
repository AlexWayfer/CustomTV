package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchReportUrlTest {
    @Test
    fun reportOpensTheChattersReportPage() {
        assertEquals("https://www.twitch.tv/some_chatter/report", twitchReportUrl("some_chatter"))
    }
}
