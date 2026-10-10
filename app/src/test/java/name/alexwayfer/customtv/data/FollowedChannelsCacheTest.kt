package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedChannelsCacheTest {
    private val live = FollowedChannel(
        id = "1",
        login = "alpha",
        displayName = "Alpha",
        avatarUrl = "https://example.com/a.png",
        isLive = true,
        categoryName = "Just Chatting",
        viewerCount = 120,
        sharedViewerCount = 300,
        collaborationCount = 2,
        collaboratorAvatarUrls = listOf("https://example.com/b.png"),
        streamTitle = "Hi",
        previewAtMillis = 1_700_000_100_000L,
    )
    private val offline = FollowedChannel(
        id = "2",
        login = "beta",
        displayName = "Beta",
        lastBroadcastAtMillis = 1_700_000_000_000L,
    )

    @Test
    fun savedListComesBackInItsOrderWithEveryField() {
        val raw = encodeFollowedChannelsCache("42", listOf(offline, live))

        assertEquals(listOf(offline, live), decodeFollowedChannelsCache(raw, "42"))
    }

    @Test
    fun anotherAccountGetsNothing() {
        val raw = encodeFollowedChannelsCache("42", listOf(live))

        assertTrue(decodeFollowedChannelsCache(raw, "7").isEmpty())
    }

    @Test
    fun brokenFileGivesNothing() {
        assertTrue(decodeFollowedChannelsCache("{not json", "42").isEmpty())
    }

    @Test
    fun streaksAreSavedNextToTheList() {
        val raw = followedCacheWithStreaks(encodeFollowedChannelsCache("42", listOf(live)), mapOf("1" to 3))!!

        assertEquals(mapOf("1" to 3), decodeFollowedWatchStreaks(raw))
        assertEquals(listOf(live), decodeFollowedChannelsCache(raw, "42"))
    }

    @Test
    fun sameStreaksLeaveTheFileAsIs() {
        val raw = followedCacheWithStreaks(encodeFollowedChannelsCache("42", listOf(live)), mapOf("1" to 3))!!

        assertNull(followedCacheWithStreaks(raw, mapOf("1" to 3)))
    }

    @Test
    fun noStreaksRemoveTheSavedOnes() {
        val raw = followedCacheWithStreaks(encodeFollowedChannelsCache("42", listOf(live)), mapOf("1" to 3))!!

        assertTrue(decodeFollowedWatchStreaks(followedCacheWithStreaks(raw, emptyMap())!!).isEmpty())
    }

    @Test
    fun newListOfTheSameAccountKeepsTheStreaks() {
        val previous = followedCacheWithStreaks(encodeFollowedChannelsCache("42", listOf(live)), mapOf("1" to 3))

        val raw = encodeFollowedChannelsCache("42", listOf(offline, live), previous)

        assertEquals(mapOf("1" to 3), decodeFollowedWatchStreaks(raw))
    }

    @Test
    fun listOfAnotherAccountDropsTheStreaks() {
        val previous = followedCacheWithStreaks(encodeFollowedChannelsCache("42", listOf(live)), mapOf("1" to 3))

        val raw = encodeFollowedChannelsCache("7", listOf(live), previous)

        assertTrue(decodeFollowedWatchStreaks(raw).isEmpty())
    }

    @Test
    fun entryWithoutIdIsSkipped() {
        val raw = """{"userId":"42","channels":[{"login":"x"},{"id":"2","login":"beta","displayName":"Beta"}]}"""

        assertEquals(listOf(FollowedChannel("2", "beta", "Beta")), decodeFollowedChannelsCache(raw, "42"))
    }

    @Test
    fun savedListNamesItsAccount() {
        assertEquals("42", decodeFollowedChannelsCacheOwner(encodeFollowedChannelsCache("42", listOf(live))))
    }

    @Test
    fun unreadableOrAnonymousListNamesNoAccount() {
        assertNull(decodeFollowedChannelsCacheOwner("not json"))
        assertNull(decodeFollowedChannelsCacheOwner("""{"channels":[]}"""))
    }

    @Test
    fun savedListOfTheSavedAccountShowsAtStart() {
        val saved = SavedFollows("42", listOf(live))

        assertEquals(saved, followsShownAtStart(saved, sessionUserId = "42", followsCanLoad = true))
    }

    @Test
    fun anotherAccountsListStaysHiddenAtStart() {
        assertNull(followsShownAtStart(SavedFollows("42", listOf(live)), sessionUserId = "7", followsCanLoad = true))
    }

    @Test
    fun listStaysHiddenAtStartWithoutASession() {
        assertNull(followsShownAtStart(SavedFollows("42", listOf(live)), sessionUserId = null, followsCanLoad = false))
    }

    @Test
    fun listStaysHiddenAtStartWhenFollowsCanNoLongerLoad() {
        assertNull(followsShownAtStart(SavedFollows("42", listOf(live)), sessionUserId = "42", followsCanLoad = false))
    }

    @Test
    fun emptySavedListShowsNothingAtStart() {
        assertNull(followsShownAtStart(SavedFollows("42", emptyList()), sessionUserId = "42", followsCanLoad = true))
    }
}
