package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPageReloadsTest {
    @Test
    fun theFirstReloadWaitsTenSeconds() {
        assertEquals(10_000L, PlayerPageReloads.delayMs(0))
    }

    @Test
    fun theSecondReloadWaitsTwentySeconds() {
        assertEquals(20_000L, PlayerPageReloads.delayMs(1))
    }

    @Test
    fun theLastDelayRepeatsSoThePageNeverStopsTrying() {
        assertEquals(20_000L, PlayerPageReloads.delayMs(2))
        assertEquals(20_000L, PlayerPageReloads.delayMs(50))
    }

    @Test
    fun aLoadThatFailedAtOnceWaitsUntilTenSecondsAfterItsStart() {
        assertEquals(10_000L, PlayerPageReloads.failedReloadDelayMs(sinceLoadMs = 0))
        assertEquals(9_750L, PlayerPageReloads.failedReloadDelayMs(sinceLoadMs = 250))
    }

    @Test
    fun aLoadThatHungTenSecondsOrMoreReloadsAtOnce() {
        assertEquals(0L, PlayerPageReloads.failedReloadDelayMs(sinceLoadMs = 10_000))
        assertEquals(0L, PlayerPageReloads.failedReloadDelayMs(sinceLoadMs = 12_430))
    }

    @Test
    fun aPageThatHasNotPlayedIsReloadedEvenWhenTwitchsPlayerIsReady() {
        assertNull(noPlaybackReloadSkip())
    }

    @Test
    fun aPageThatPlayedIsNotReloaded() {
        assertEquals("it has played", noPlaybackReloadSkip(played = true))
    }

    @Test
    fun aPageThatExplainsTheMissingVideoIsNotReloaded() {
        assertEquals("the page explains the missing video", noPlaybackReloadSkip(withheld = true))
    }

    @Test
    fun aPageTheViewerPausedIsNotReloadedForMissingPlayback() {
        assertEquals("the viewer paused it", noPlaybackReloadSkip(explicitlyPaused = true))
    }

    private fun noPlaybackReloadSkip(
        played: Boolean = false,
        withheld: Boolean = false,
        explicitlyPaused: Boolean = false,
    ) = PlayerPageReloads.noPlaybackReloadSkip(played, withheld, explicitlyPaused)

    @Test
    fun aPageWhosePlaylistCameWaitsOnceMoreInsteadOfReloading() {
        assertTrue(PlayerPageReloads.waitsOnceMore(playlistLoaded = true, waitedOnceMore = false))
    }

    @Test
    fun aPageThatAlreadyWaitedOnceMoreIsReloaded() {
        assertFalse(PlayerPageReloads.waitsOnceMore(playlistLoaded = true, waitedOnceMore = true))
    }

    @Test
    fun aPageWithoutItsPlaylistIsReloadedWithoutWaiting() {
        assertFalse(PlayerPageReloads.waitsOnceMore(playlistLoaded = false, waitedOnceMore = false))
    }

    @Test
    fun aPlaylistThatCameCountsAsLoaded() {
        assertTrue(PlayerPageReloads.playlistAnswerCounts(code = 200))
    }

    @Test
    fun anOfflineOrSubscriberOnlyAnswerCountsAsLoaded() {
        assertTrue(PlayerPageReloads.playlistAnswerCounts(code = 404))
        assertTrue(PlayerPageReloads.playlistAnswerCounts(code = 403))
    }

    @Test
    fun aServerFailureDoesNotCountAsLoaded() {
        assertFalse(PlayerPageReloads.playlistAnswerCounts(code = 500))
        assertFalse(PlayerPageReloads.playlistAnswerCounts(code = 429))
    }

    @Test
    fun theWaitOnceMoreIsTheNextReloadsDelay() {
        assertEquals(20_000L, PlayerPageReloads.onceMoreDelayMs(attemptsMade = 0))
        assertEquals(20_000L, PlayerPageReloads.onceMoreDelayMs(attemptsMade = 3))
    }

    @Test
    fun hiddenVideoAfterTheErrorReloadReloadsThePage() {
        assertTrue(PlayerPageReloads.reloadsAfterErrorReload(videoShown = false, userPaused = false))
    }

    @Test
    fun videoThatCameBackIsNotReloaded() {
        assertFalse(PlayerPageReloads.reloadsAfterErrorReload(videoShown = true, userPaused = false))
    }

    @Test
    fun aPausedPlayerIsNotReloaded() {
        assertFalse(PlayerPageReloads.reloadsAfterErrorReload(videoShown = false, userPaused = true))
    }
}
