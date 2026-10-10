package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.auth.TwitchProfileLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDetailsTest {
    private fun profile(id: String?, login: String = "user$id", banner: String? = null) = ProfileDetails(
        userId = id,
        login = login,
        displayName = login,
        avatarUrl = null,
        bannerUrl = banner,
        description = null,
        followerCount = null,
        createdAtMillis = null,
        links = null,
    )

    @Test
    fun oneAnswerFeedsTheCardAndTheProfile() {
        val details = parseProfileDetails(
            """
            {"data":{"user":{"id":"7","login":"ada","displayName":"Ada","profileImageURL":"https://example/a.png",
            "bannerImageURL":"https://example/b.png","description":" Hi ","createdAt":"2015-07-21T00:00:00Z",
            "followers":{"totalCount":12},"channel":{"socialMedias":[{"name":"x","title":"X","url":"https://x.com/ada"}]}}}}
            """.trimIndent(),
            fallbackLogin = "ada",
        )!!
        assertEquals("7", details.userId)
        assertEquals("https://example/b.png", details.bannerUrl)
        assertEquals("Hi", details.description)
        assertEquals(12, details.followerCount)
        val card = details.toChatterProfile()
        assertEquals("https://example/a.png", card.avatarUrl)
        assertEquals(1437436800000L, card.createdAtMillis)
        assertEquals(listOf(TwitchProfileLink("X", "https://x.com/ada", "x")), card.links)
    }

    @Test
    fun anOfflineChannelShowsWhenItsLastStreamStarted() {
        val details = parseProfileDetails(
            """{"data":{"user":{"login":"ada","stream":null,"lastBroadcast":{"startedAt":"2023-08-08T00:00:00Z"}}}}""",
            fallbackLogin = "ada",
        )!!
        assertEquals(1691452800000L, details.lastLiveMillis)
    }

    @Test
    fun aLiveChannelHasNoLastLiveDate() {
        val details = parseProfileDetails(
            """{"data":{"user":{"login":"ada","stream":{"id":"1"},"lastBroadcast":{"startedAt":"2023-08-08T00:00:00Z"}}}}""",
            fallbackLogin = "ada",
        )!!
        assertNull(details.lastLiveMillis)
    }

    @Test
    fun aChannelThatNeverStreamedHasNoLastLiveDate() {
        val details = parseProfileDetails(
            """{"data":{"user":{"login":"ada","stream":null,"lastBroadcast":{"startedAt":null}}}}""",
            fallbackLogin = "ada",
        )!!
        assertNull(details.lastLiveMillis)
    }

    @Test
    fun anOpenedProfileMovesFirstAndReplacesItsOlderCopy() {
        val kept = listOf(profile("1"), profile("2", banner = "old"), profile("3"))
        val opened = profile("2", banner = "new")
        assertEquals(listOf("2", "1", "3"), profilesAfterOpen(kept, opened).map { it.userId })
        assertEquals("new", profilesAfterOpen(kept, opened).first().bannerUrl)
    }

    @Test
    fun theProfileOpenedLongestAgoLeavesWhenTheListIsFull() {
        val kept = listOf(profile("1"), profile("2"), profile("3"))
        assertEquals(listOf("4", "1", "2"), profilesAfterOpen(kept, profile("4"), kept = 3).map { it.userId })
    }

    @Test
    fun aProfileWithoutAUserIdIsNotKept() {
        val kept = listOf(profile("1"))
        assertEquals(kept, profilesAfterOpen(kept, profile(null, login = "ghost")))
    }

    @Test
    fun aKeptProfileIsFoundByItsLoginInAnyCase() {
        val kept = listOf(profile("1", login = "ada"))
        assertEquals("1", profileByLogin(kept, "ADA")?.userId)
        assertNull(profileByLogin(kept, "bob"))
    }

    @Test
    fun keptProfilesReadBackFromTheFile() {
        val kept = listOf(
            profile("1", login = "ada", banner = "https://example/b.png").copy(
                followerCount = 3,
                lastLiveMillis = 1691452800000L,
                links = listOf(TwitchProfileLink("X", "https://x.com/ada", "x")),
            ),
            profile("2", login = "bob"),
        )
        assertEquals(kept, parseStoredProfiles(profilesJson(kept)))
    }
}
