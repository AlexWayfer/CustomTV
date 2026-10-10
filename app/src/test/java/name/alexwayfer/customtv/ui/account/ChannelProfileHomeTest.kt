package name.alexwayfer.customtv.ui.account

import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.ChannelProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelProfileHomeTest {
    private val offline = ChannelProfile("channel", "Channel", null)
    private val live = offline.copy(isLive = true)

    private val offlineSuggestion = ChannelProfile("shelik", "Shelik", null)
    private val liveSuggestion = ChannelProfile("ilame", "iLame", null, isLive = true)
    private val laterLiveSuggestion = ChannelProfile("lirik", "LIRIK", null, isLive = true)

    @Test
    fun liveStreamTakesPrecedenceOverPastBroadcastAndSuggestions() {
        assertEquals(ProfileHomePrimary.Live, profileHomePrimary(live, video("past"), listOf(liveSuggestion)))
    }

    @Test
    fun offlineChannelShowsTheOfferedPastBroadcastBeforeSuggestions() {
        val newest = video("newest")
        assertEquals(
            ProfileHomePrimary.PastBroadcast(newest),
            profileHomePrimary(offline, newest, listOf(liveSuggestion)),
        )
    }

    @Test
    fun offlineChannelWithoutABroadcastShowsTheFirstLiveSuggestion() {
        assertEquals(
            ProfileHomePrimary.Suggested(liveSuggestion),
            profileHomePrimary(offline, null, listOf(offlineSuggestion, liveSuggestion, laterLiveSuggestion)),
        )
    }

    @Test
    fun offlineSuggestionsOnlyLeaveNoPrimaryContent() {
        assertEquals(ProfileHomePrimary.None, profileHomePrimary(offline, null, listOf(offlineSuggestion)))
    }

    @Test
    fun offlineChannelWithoutABroadcastOrSuggestionsHasNoPrimaryContent() {
        assertEquals(ProfileHomePrimary.None, profileHomePrimary(offline, null, emptyList()))
    }

    @Test
    fun recentCategoriesStartWithTheLastChapterOfTheNewestRecording() {
        val categories = recentCategories(
            videos = listOf(video("newest"), video("older")),
            chapters = mapOf(
                "newest" to VodCategories(
                    chapters = listOf(
                        VodChapter(0.0, "Just Chatting", "jc.jpg"),
                        VodChapter(600.0, "Dota 2", "dota.jpg"),
                    ),
                    fallbackCategory = "Dota 2",
                ),
                "older" to VodCategories(
                    chapters = listOf(VodChapter(0.0, "Art", "art.jpg"), VodChapter(60.0, "Just Chatting", "jc.jpg")),
                    fallbackCategory = "Just Chatting",
                ),
            ),
        )

        assertEquals(listOf("Dota 2", "Just Chatting", "Art"), categories.map(RecentCategory::name))
        assertEquals("dota.jpg", categories.first().boxArtUrl)
    }

    @Test
    fun aRecordingWithoutChaptersCountsItsOwnCategory() {
        val categories = recentCategories(
            videos = listOf(video("only")),
            chapters = mapOf("only" to VodCategories(emptyList(), "Minecraft", "mc.jpg")),
        )

        assertEquals(listOf(RecentCategory("Minecraft", "mc.jpg")), categories)
    }

    @Test
    fun aRecordingWithoutLoadedChaptersAddsNoCategory() {
        assertEquals(emptyList<RecentCategory>(), recentCategories(listOf(video("unknown")), emptyMap()))
    }

    @Test
    fun aPhoneGridKeepsThreeColumnsAndAWideScreenAddsMore() {
        assertEquals(3, categoryGridColumns(360.dp))
        assertEquals(3, categoryGridColumns(200.dp))
        assertEquals(6, categoryGridColumns(800.dp))
    }

    private fun video(id: String) = TwitchVideo(id, id, null, null, null, 0, 0)
}
