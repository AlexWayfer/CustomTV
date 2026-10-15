package name.alexwayfer.customtv.ui.account

import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.TwitchCategory
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
                        VodChapter(0.0, "Just Chatting", "jc.jpg", categoryId = "509658"),
                        VodChapter(600.0, "Dota 2", "dota.jpg", categoryId = "29595"),
                    ),
                    fallbackCategory = "Dota 2",
                ),
                "older" to VodCategories(
                    chapters = listOf(
                        VodChapter(0.0, "Art", "art.jpg", categoryId = "509660"),
                        VodChapter(60.0, "Just Chatting", "jc.jpg", categoryId = "509658"),
                    ),
                    fallbackCategory = "Just Chatting",
                ),
            ),
        )

        assertEquals(listOf("Dota 2", "Just Chatting", "Art"), categories.map(TwitchCategory::name))
        assertEquals("dota.jpg", categories.first().boxArtUrl)
    }

    @Test
    fun aRecordingWithoutChaptersCountsItsOwnCategory() {
        val categories = recentCategories(
            videos = listOf(video("only")),
            chapters = mapOf("only" to VodCategories(emptyList(), "Minecraft", "mc.jpg", fallbackCategoryId = "27471")),
        )

        assertEquals(listOf(TwitchCategory("Minecraft", "mc.jpg", id = "27471")), categories)
    }

    @Test
    fun aChapterWithoutAGameIsNoCategory() {
        val categories = recentCategories(
            videos = listOf(video("only")),
            chapters = mapOf(
                "only" to VodCategories(
                    chapters = listOf(
                        VodChapter(0.0, "Chapter 1"),
                        VodChapter(1977.0, "Just Chatting", "jc.jpg", categoryId = "509658"),
                    ),
                    fallbackCategory = null,
                ),
            ),
        )

        assertEquals(listOf(TwitchCategory("Just Chatting", "jc.jpg", id = "509658")), categories)
    }

    @Test
    fun aRecordingWithOnlyGamelessChaptersAndNoGameAddsNoCategory() {
        val categories = recentCategories(
            videos = listOf(video("only")),
            chapters = mapOf("only" to VodCategories(listOf(VodChapter(0.0, "Chapter 1")), fallbackCategory = null)),
        )

        assertEquals(emptyList<TwitchCategory>(), categories)
    }

    @Test
    fun aLaterRecordingFillsInTheBoxArtAnEarlierOneLacked() {
        val categories = recentCategories(
            videos = listOf(video("newest"), video("older")),
            chapters = mapOf(
                "newest" to VodCategories(listOf(VodChapter(0.0, "Art", null, categoryId = "509660")), "Art"),
                "older" to VodCategories(listOf(VodChapter(0.0, "Art", "art.jpg", categoryId = "509660")), "Art"),
            ),
        )

        assertEquals(listOf(TwitchCategory("Art", "art.jpg", id = "509660")), categories)
    }

    @Test
    fun aRecordingWithoutLoadedChaptersAddsNoCategory() {
        assertEquals(emptyList<TwitchCategory>(), recentCategories(listOf(video("unknown")), emptyMap()))
    }

    private fun video(id: String) = TwitchVideo(id, id, null, null, null, 0, 0)
}
