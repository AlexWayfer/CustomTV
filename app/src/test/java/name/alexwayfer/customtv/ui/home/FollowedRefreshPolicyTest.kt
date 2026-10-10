package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedRefreshPolicyTest {
    @Test
    fun theFirstAutomaticLoadIsAllowed() {
        assertTrue(
            followedAutomaticRefreshAllowed(
                lastAtMillis = null,
                nowMillis = 1_000L,
                lastUserId = null,
                userId = "me",
            ),
        )
    }

    @Test
    fun minimizingAgainWithinAMinuteDoesNotReload() {
        assertFalse(
            followedAutomaticRefreshAllowed(
                lastAtMillis = 1_000L,
                nowMillis = 2_000L,
                lastUserId = "me",
                userId = "me",
            ),
        )
    }

    @Test
    fun minimizingAgainAfterAMinuteReloads() {
        assertTrue(
            followedAutomaticRefreshAllowed(
                lastAtMillis = 1_000L,
                nowMillis = 61_000L,
                lastUserId = "me",
                userId = "me",
            ),
        )
    }

    @Test
    fun aDifferentAccountReloadsImmediately() {
        assertTrue(
            followedAutomaticRefreshAllowed(
                lastAtMillis = 1_000L,
                nowMillis = 2_000L,
                lastUserId = "me",
                userId = "other",
            ),
        )
    }

    @Test
    fun theListOfTheSameAccountStays() {
        assertFalse(followedListBelongsToAnotherAccount(shownUserId = "me", userId = "me"))
    }

    @Test
    fun theListOfTheLoggedOutAccountIsDroppedForTheNewOne() {
        assertTrue(followedListBelongsToAnotherAccount(shownUserId = "me", userId = "other"))
    }

    @Test
    fun nothingIsDroppedBeforeTheFirstLoad() {
        assertFalse(followedListBelongsToAnotherAccount(shownUserId = null, userId = "me"))
    }

    @Test
    fun anEmptyListShowsNamesAsTheyArrive() {
        val names = listOf(FollowedChannel(id = "1", login = "amy", displayName = "amy"))
        assertEquals(names, followedListDuringReload(emptyList(), names, reloadFinished = false))
    }

    @Test
    fun anExistingListKeepsLiveCategoryAndLastBroadcastUntilTheReloadFinishes() {
        val current = listOf(
            FollowedChannel(
                id = "1",
                login = "amy",
                displayName = "amy",
                isLive = true,
                categoryName = "Just Chatting",
                viewerCount = 10,
                lastBroadcastAtMillis = 5_000L,
            ),
        )
        val namesOnly = listOf(FollowedChannel(id = "1", login = "amy", displayName = "amy"))
        assertSame(current, followedListDuringReload(current, namesOnly, reloadFinished = false))
    }

    @Test
    fun aFinishedReloadReplacesTheList() {
        val current = listOf(
            FollowedChannel(id = "1", login = "amy", displayName = "amy", isLive = true, categoryName = "Old"),
        )
        val ready = listOf(
            FollowedChannel(id = "1", login = "amy", displayName = "amy", isLive = true, categoryName = "New"),
        )
        assertEquals(ready, followedListDuringReload(current, ready, reloadFinished = true))
    }

    @Test
    fun loadedFollowsGiveTheirChannelIds() {
        val channels = listOf(
            FollowedChannel(id = "1", login = "amy", displayName = "amy"),
            FollowedChannel(id = "2", login = "bob", displayName = "bob"),
        )
        assertEquals(
            setOf("1", "2"),
            followedChannelIds(signedIn = true, status = FollowedChannelsStatus.Ready, channels = channels),
        )
    }

    @Test
    fun anEmptyLoadedListIsKnownToFollowNobody() {
        assertEquals(
            emptySet<String>(),
            followedChannelIds(signedIn = true, status = FollowedChannelsStatus.Ready, channels = emptyList()),
        )
    }

    @Test
    fun followsAreUnknownWhileLoadingOrFailedOrLoggedOut() {
        val channels = listOf(FollowedChannel(id = "1", login = "amy", displayName = "amy"))
        assertNull(followedChannelIds(signedIn = true, status = FollowedChannelsStatus.Loading, channels = emptyList()))
        assertNull(followedChannelIds(signedIn = true, status = FollowedChannelsStatus.Unavailable, channels = emptyList()))
        assertNull(followedChannelIds(signedIn = true, status = FollowedChannelsStatus.NeedsPermission, channels = emptyList()))
        assertNull(followedChannelIds(signedIn = false, status = FollowedChannelsStatus.Ready, channels = channels))
    }
}
