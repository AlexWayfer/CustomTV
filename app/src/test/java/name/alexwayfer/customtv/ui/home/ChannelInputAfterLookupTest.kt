package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.ChannelProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelInputAfterLookupTest {
    @Test
    fun aFoundChannelClearsTheInput() {
        assertEquals("", channelInputAfterLookup("alexwayfer", ChannelLookup.Found(profile())))
    }

    @Test
    fun aMissingChannelKeepsTheInput() {
        assertEquals("missing", channelInputAfterLookup("missing", ChannelLookup.NotFound))
    }

    @Test
    fun anUnavailableLookupKeepsTheInput() {
        assertEquals("alexwayfer", channelInputAfterLookup("alexwayfer", ChannelLookup.Unavailable))
    }

    private fun profile() = ChannelProfile(
        id = "1",
        login = "alexwayfer",
        displayName = "AlexWayfer",
        avatarUrl = "",
    )
}
