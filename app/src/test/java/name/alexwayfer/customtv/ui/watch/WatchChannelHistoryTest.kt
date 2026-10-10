package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchChannelHistoryTest {
    @Test
    fun backOnAFollowUpChannelReturnsToThePreviousChannel() {
        assertTrue(watchBackReturnsToPreviousChannel(WATCH_CHANNEL_ROUTE))
    }

    @Test
    fun backOnTheFirstChannelMinimizesInsteadOfReturning() {
        assertFalse(watchBackReturnsToPreviousChannel("home"))
        assertFalse(watchBackReturnsToPreviousChannel(null))
    }
}
