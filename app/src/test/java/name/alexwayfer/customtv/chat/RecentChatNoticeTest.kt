package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChannelLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentChatNoticeTest {
    @Test
    fun anEmptyRecentHistoryDoesNotShowANotice() {
        assertNull(recentChatNotice(RecentChatLoadResult()))
    }

    @Test
    fun aFailedRecentHistoryLoadShowsAnErrorNotice() {
        assertEquals(
            ChatNotice.RecentChatFailed,
            recentChatNotice(RecentChatLoadResult(failed = true)),
        )
    }

    @Test
    fun aMissingChannelShowsAWarningNotice() {
        assertEquals(ChatNotice.ChannelNotFound, channelLookupNotice(ChannelLookup.NotFound))
    }

    @Test
    fun anUnavailableChannelLookupDoesNotClaimTheChannelIsMissing() {
        assertNull(channelLookupNotice(ChannelLookup.Unavailable))
    }
}
