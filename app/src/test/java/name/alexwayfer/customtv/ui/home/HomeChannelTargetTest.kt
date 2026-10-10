package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChannelTargetTest {
    @Test
    fun liveChannelOpensTheStream() {
        assertEquals(HomeChannelTarget.Stream, homeChannelTarget(live = true))
    }

    @Test
    fun offlineChannelOpensTheProfile() {
        assertEquals(HomeChannelTarget.Profile, homeChannelTarget(live = false))
    }
}
