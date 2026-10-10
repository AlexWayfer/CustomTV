package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.FollowedChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelSearchSuggestionsTest {
    private val history = listOf("food", "foo", "bar")
    private val noProfiles = emptyMap<String, ChannelProfile>()

    @Test
    fun anEmptyFieldListsRecentChannelsInOrder() {
        assertEquals(history, channelSearchSuggestions(history, noProfiles, "  "))
    }

    @Test
    fun aPrefixKeepsMatchingChannels() {
        assertEquals(listOf("food", "foo"), channelSearchSuggestions(history, noProfiles, "fo"))
    }

    @Test
    fun aChannelUrlStillMatchesTheLogin() {
        assertEquals(listOf("bar"), channelSearchSuggestions(history, noProfiles, "https://twitch.tv/@Bar"))
    }

    @Test
    fun aQueryWithNoMatchListsNothing() {
        assertEquals(emptyList<String>(), channelSearchSuggestions(history, noProfiles, "zzz"))
    }

    @Test
    fun aRecentChannelMatchesByTheStartOfItsDisplayName() {
        val profiles = mapOf("bar" to ChannelProfile(login = "bar", displayName = "Жарко", avatarUrl = null))
        assertEquals(listOf("bar"), channelSearchSuggestions(history, profiles, "жар"))
    }

    @Test
    fun typingSuggestsMatchingFollowedChannelsLiveFirstThenTheOnesThatStreamedLast() {
        val followed = listOf(
            followed("zara", "Zara", lastBroadcast = 100),
            followed("zed", "Zed", live = true),
            followed("zeta", "Zeta", lastBroadcast = 200),
            followed("other", "Other", live = true),
        )
        assertEquals(
            listOf("zed", "zeta", "zara"),
            followedSearchSuggestions(followed, shown = emptyList(), query = "z").map { it.login },
        )
    }

    @Test
    fun followedChannelsAlreadyInTheRecentsAreLeftOut() {
        val followed = listOf(followed("foo", "Foo"), followed("fox", "Fox"))
        assertEquals(
            listOf("fox"),
            followedSearchSuggestions(followed, shown = listOf("Foo"), query = "fo").map { it.login },
        )
    }

    @Test
    fun aFollowedChannelMatchesByTheStartOfItsDisplayName() {
        val followed = listOf(followed("abc", "Жарко"))
        assertEquals(
            listOf("abc"),
            followedSearchSuggestions(followed, shown = emptyList(), query = "Жа").map { it.login },
        )
    }

    @Test
    fun typingSuggestsEveryMatchWithoutALimit() {
        val followed = (1..30).map { followed("a$it", "A$it") }
        assertEquals(30, followedSearchSuggestions(followed, shown = emptyList(), query = "a").size)
    }

    @Test
    fun anEmptyFieldFillsUpWithTheOfflineChannelsThatStreamedLast() {
        val followed = listOf(
            followed("old", "Old", lastBroadcast = 100),
            followed("live", "Live", live = true),
            followed("never", "Never"),
            followed("recent", "Recent", lastBroadcast = 300),
        )
        assertEquals(
            listOf("live", "recent", "old", "never"),
            followedSearchSuggestions(followed, shown = emptyList(), query = "").map { it.login },
        )
    }

    @Test
    fun anEmptyFieldStopsAtTheLimit() {
        val followed = (1..30).map { followed("c$it", "C$it", lastBroadcast = it.toLong()) }
        val suggested = followedSearchSuggestions(followed, shown = emptyList(), query = "")
        assertEquals(EMPTY_SEARCH_FOLLOWED, suggested.size)
        assertEquals("c30", suggested.first().login)
    }

    @Test
    fun anEmptyFieldShowsEveryLiveChannelEvenPastTheLimit() {
        val live = (1..12).map { followed("l$it", "L$it", live = true) }
        val offline = listOf(followed("off", "Off", lastBroadcast = 1))
        val suggested = followedSearchSuggestions(live + offline, shown = emptyList(), query = "")
        assertEquals(12, suggested.size)
        assertTrue(suggested.all { it.isLive })
    }

    @Test
    fun aLiveProfileShowsTheLiveMark() {
        assertTrue(searchHistoryIsLive(profileLive = true, followedLive = false))
    }

    @Test
    fun aFollowedLiveChannelShowsTheLiveMark() {
        assertTrue(searchHistoryIsLive(profileLive = false, followedLive = true))
    }

    @Test
    fun anOfflineChannelHidesTheLiveMark() {
        assertFalse(searchHistoryIsLive(profileLive = false, followedLive = false))
    }

    private fun followed(login: String, displayName: String, live: Boolean = false, lastBroadcast: Long? = null) =
        FollowedChannel(
            id = login,
            login = login,
            displayName = displayName,
            isLive = live,
            lastBroadcastAtMillis = lastBroadcast,
        )
}
