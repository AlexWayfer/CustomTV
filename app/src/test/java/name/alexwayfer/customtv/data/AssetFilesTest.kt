package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AssetFilesTest {
    private val now = 100.days.inWholeMilliseconds

    @Test
    fun channelOpenedMoreThanAWeekAgoIsStale() {
        val stale = staleChannelFolders(
            mapOf(
                "1" to now - 8.days.inWholeMilliseconds,
                "2" to now - 1.hours.inWholeMilliseconds,
                "3" to now - 30.days.inWholeMilliseconds,
            ),
            now,
        )

        assertEquals(listOf("1", "3"), stale)
    }

    @Test
    fun channelOpenedExactlyAWeekAgoStays() {
        val stale = staleChannelFolders(mapOf("1" to now - 7.days.inWholeMilliseconds), now)

        assertEquals(emptyList<String>(), stale)
    }

    @Test
    fun noChannelsLeaveNothingToRemove() {
        assertEquals(emptyList<String>(), staleChannelFolders(emptyMap(), now))
    }
}
