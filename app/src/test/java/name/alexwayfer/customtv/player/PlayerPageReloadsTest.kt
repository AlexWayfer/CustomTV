package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(noPlaybackReloads())
    }

    @Test
    fun aPageThatPlayedIsNotReloaded() {
        assertFalse(noPlaybackReloads(played = true))
    }

    @Test
    fun aPageThatExplainsTheMissingVideoIsNotReloaded() {
        assertFalse(noPlaybackReloads(withheld = true))
    }

    @Test
    fun aPageTheViewerPausedIsNotReloadedForMissingPlayback() {
        assertFalse(noPlaybackReloads(userPaused = true))
    }

    private fun noPlaybackReloads(
        played: Boolean = false,
        withheld: Boolean = false,
        userPaused: Boolean = false,
    ) = PlayerPageReloads.reloadsWithoutPlayback(played, withheld, userPaused)

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
