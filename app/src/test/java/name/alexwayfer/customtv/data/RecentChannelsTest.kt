package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentChannelsTest {
    @Test
    fun savedChannelsRoundTrip() {
        val channels = listOf(RecentChannel("1", "foo"), RecentChannel("2", "bar"))

        assertEquals(channels, parseRecentChannels(recentChannelsJoined(channels)))
    }

    @Test
    fun aBareLoginFromTheOldFormatIsSkipped() {
        assertEquals(listOf(RecentChannel("2", "bar")), parseRecentChannels("foo\n2 bar"))
    }

    @Test
    fun anEmptyValueHasNoChannels() {
        assertEquals(emptyList<RecentChannel>(), parseRecentChannels(""))
    }

    @Test
    fun aRenamedChannelMovesToTheTopAsOneEntry() {
        val current = listOf(RecentChannel("1", "foo"), RecentChannel("2", "bar"))

        assertEquals(
            listOf(RecentChannel("2", "baz"), RecentChannel("1", "foo")),
            recentChannelsAfterAdd(current, RecentChannel("2", "Baz")),
        )
    }

    @Test
    fun theListKeepsFiftyChannels() {
        val current = (1..50).map { RecentChannel("$it", "c$it") }

        val updated = recentChannelsAfterAdd(current, RecentChannel("51", "new"))

        assertEquals(50, updated.size)
        assertEquals(RecentChannel("51", "new"), updated.first())
        assertEquals(RecentChannel("49", "c49"), updated.last())
    }

    @Test
    fun aRefreshTakesTheCurrentLoginOfEachId() {
        val current = listOf(RecentChannel("1", "foo"), RecentChannel("2", "bar"))

        assertEquals(
            listOf(RecentChannel("1", "renamed"), RecentChannel("2", "bar")),
            recentChannelsWithLogins(current, mapOf("1" to "Renamed", "3" to "other")),
        )
    }
}
