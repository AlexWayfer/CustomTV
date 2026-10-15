package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRecordingsTest {
    private fun video(id: String) = TwitchVideo(id, "", null, null, null, 0L, 0L)

    private val art = VodCategories(emptyList(), "Art")

    @Test
    fun chaptersThatLoadedAgainJoinThePageAndStopBeingFailed() {
        val page = ChannelRecordingsPage(
            videos = listOf(video("1"), video("2")),
            chapters = emptyMap(),
            chaptersFailed = setOf("1", "2"),
        )

        val after = pageWithChapters(page, asked = setOf("1", "2"), loaded = mapOf("1" to art))

        assertEquals(mapOf("1" to art), after.chapters)
        assertEquals(setOf("2"), after.chaptersFailed)
    }

    @Test
    fun chaptersThatFailedTheFirstTimeAndAgainBecomeFailed() {
        val page = ChannelRecordingsPage(listOf(video("1"), video("2")), mapOf("2" to art))

        val after = pageWithChapters(page, asked = setOf("1"), loaded = emptyMap())

        assertEquals(setOf("1"), after.chaptersFailed)
        assertEquals(mapOf("2" to art), after.chapters)
    }

    @Test
    fun aRecordingThePageNoLongerListsIsLeftOut() {
        val page = ChannelRecordingsPage(listOf(video("1")), emptyMap())

        val after = pageWithChapters(page, asked = setOf("1", "old"), loaded = mapOf("old" to art))

        assertEquals(emptyMap<String, VodCategories>(), after.chapters)
        assertEquals(setOf("1"), after.chaptersFailed)
    }
}
