package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLogsBrowserTest {
    @Test
    fun aChannelLoginProducesItsLurkologyLogsPage() {
        assertEquals("https://lurkology.com/logs?u=alex_wayfer", messageLogsUrl("#Alex_Wayfer "))
    }
}
