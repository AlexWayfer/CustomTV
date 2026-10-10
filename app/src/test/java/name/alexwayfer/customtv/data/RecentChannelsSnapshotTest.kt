package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentChannelsSnapshotTest {
    private val liveStream = RecentStream(
        isLive = true,
        streamTitle = "Hi",
        categoryName = "Just Chatting",
        streamStartedAtMillis = 1_700_000_000_000L,
        viewerCount = 120,
        sharedViewerCount = 300,
        collaborationCount = 2,
        collaboratorAvatarUrls = listOf("https://example.com/b.png"),
    )
    private val restored = ChannelProfile(login = "alpha", displayName = "Alpha", avatarUrl = "a.png", id = "1")

    @Test
    fun savedStreamsComeBackWithEveryField() {
        val streams = mapOf("1" to liveStream, "2" to RecentStream(isLive = false))

        assertEquals(streams, decodeRecentStreams(encodeRecentStreams(streams)))
    }

    @Test
    fun anUnreadableFileGivesNothing() {
        assertTrue(decodeRecentStreams("not json").isEmpty())
    }

    @Test
    fun aRestoredProfileShowsItsSavedStream() {
        val shown = recentProfileForDisplay(restored, mapOf("1" to liveStream)) { false }

        assertEquals(restored.withRecentStream(liveStream), shown)
        assertEquals("Alpha", shown.displayName)
        assertTrue(shown.isLive)
    }

    @Test
    fun aProfileLoadedInThisRunKeepsItsCurrentStream() {
        val loaded = restored.copy(isLive = false)

        assertEquals(loaded, recentProfileForDisplay(loaded, mapOf("1" to liveStream)) { true })
    }

    @Test
    fun aProfileWithoutASavedStreamStaysAsItIs() {
        assertEquals(restored, recentProfileForDisplay(restored, emptyMap()) { false })
    }

    @Test
    fun aLoadedChannelSavesItsCurrentStream() {
        val loaded = restored.copy(isLive = false)

        val streams = recentStreamsAfterRefresh(mapOf("1" to liveStream), listOf("1"), listOf(loaded)) { true }

        assertEquals(mapOf("1" to RecentStream(isLive = false)), streams)
    }

    @Test
    fun aChannelTheLoadMissedKeepsItsSavedStream() {
        val streams = recentStreamsAfterRefresh(mapOf("1" to liveStream), listOf("1"), listOf(restored)) { false }

        assertEquals(mapOf("1" to liveStream), streams)
    }

    @Test
    fun aChannelNoLongerInTheRecentsIsDropped() {
        val streams = recentStreamsAfterRefresh(mapOf("1" to liveStream, "9" to liveStream), listOf("1"), emptyList()) {
            false
        }

        assertEquals(mapOf("1" to liveStream), streams)
    }
}
