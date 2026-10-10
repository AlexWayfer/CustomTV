package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedChannelsSectionTest {
    @Test
    fun runningLoadIsLoading() {
        assertTrue(followedChannelsLoading(refreshing = true, status = FollowedChannelsStatus.Ready))
    }

    @Test
    fun firstLoadIsLoadingBeforeItRuns() {
        assertTrue(followedChannelsLoading(refreshing = false, status = FollowedChannelsStatus.Loading))
    }

    @Test
    fun askingForThePermissionAgainIsLoading() {
        assertTrue(followedChannelsLoading(refreshing = false, status = FollowedChannelsStatus.NeedsPermission))
    }

    @Test
    fun loadedListIsNotLoading() {
        assertFalse(followedChannelsLoading(refreshing = false, status = FollowedChannelsStatus.Ready))
    }

    @Test
    fun unavailableListIsNotLoading() {
        assertFalse(followedChannelsLoading(refreshing = false, status = FollowedChannelsStatus.Unavailable))
    }
}
