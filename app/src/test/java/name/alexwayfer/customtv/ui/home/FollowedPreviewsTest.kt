package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.player.StreamMediaArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FollowedPreviewsTest {
    private val maxAge = StreamMediaArtwork.PREVIEW_MAX_AGE.inWholeMilliseconds
    private val now = 1_700_000_000_000L
    private val live = FollowedChannel(id = "1", login = "alpha", displayName = "Alpha", isLive = true)
    private val offline = FollowedChannel(id = "2", login = "beta", displayName = "Beta")

    @Test
    fun sectionsSplitLiveFromOfflineInListOrder() {
        val secondLive = live.copy(id = "3", login = "gamma")
        val sections = followedSections(listOf(live, offline, secondLive))

        assertEquals(listOf(live, secondLive), sections.live)
        assertEquals(listOf(offline), sections.offline)
    }

    @Test
    fun liveChannelWithoutPreviewTakesTheCurrentTime() {
        assertEquals(now, followedPreviewAt(isLive = true, knownAtMillis = null, nowMillis = now))
    }

    @Test
    fun youngPreviewIsKept() {
        val taken = now - maxAge + 1
        assertEquals(taken, followedPreviewAt(isLive = true, knownAtMillis = taken, nowMillis = now))
    }

    @Test
    fun previewAtMaxAgeIsRetaken() {
        assertEquals(now, followedPreviewAt(isLive = true, knownAtMillis = now - maxAge, nowMillis = now))
    }

    @Test
    fun previewFromTheFutureIsRetaken() {
        assertEquals(now, followedPreviewAt(isLive = true, knownAtMillis = now + 1, nowMillis = now))
    }

    @Test
    fun offlineChannelHasNoPreview() {
        assertNull(followedPreviewAt(isLive = false, knownAtMillis = now, nowMillis = now))
    }

    @Test
    fun reloadedChannelKeepsThePreviewOfTheShownList() {
        val shown = live.copy(previewAtMillis = now - 1_000)

        val next = followedWithPreviews(listOf(shown), listOf(live), now)

        assertEquals(listOf(shown), next)
    }

    @Test
    fun channelThatWasOfflineGetsAFreshPreview() {
        val next = followedWithPreviews(listOf(live.copy(isLive = false)), listOf(live), now)

        assertEquals(now, next.single().previewAtMillis)
    }

    @Test
    fun channelThatWentOfflineDropsItsPreview() {
        val next = followedWithPreviews(listOf(live.copy(previewAtMillis = now)), listOf(live.copy(isLive = false)), now)

        assertNull(next.single().previewAtMillis)
    }

    @Test
    fun previewUrlCarriesThePreviewTime() {
        assertEquals(
            StreamMediaArtwork.previewUrl("alpha", now),
            followedPreviewUrl(live.copy(previewAtMillis = now)),
        )
    }
}
