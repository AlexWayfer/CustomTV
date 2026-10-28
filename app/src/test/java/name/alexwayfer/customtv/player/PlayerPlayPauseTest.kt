package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlayPauseTest {
    @Test
    fun aPausedPlayerIsPausedEvenWhileAwaitingPlayback() {
        assertEquals(
            PlayerPlayback.Paused,
            playerPlayback(paused = true, awaitingPlayback = true, playbackConfirmed = false, offline = false),
        )
    }

    @Test
    fun aPlayerAwaitingPlaybackIsStarting() {
        assertEquals(
            PlayerPlayback.Starting,
            playerPlayback(paused = false, awaitingPlayback = true, playbackConfirmed = true, offline = false),
        )
    }

    @Test
    fun aPlayerWithoutConfirmedPlaybackIsStarting() {
        assertEquals(
            PlayerPlayback.Starting,
            playerPlayback(paused = false, awaitingPlayback = false, playbackConfirmed = false, offline = false),
        )
    }

    @Test
    fun aConfirmedPlayerNotAwaitingPlaybackIsPlaying() {
        assertEquals(
            PlayerPlayback.Playing,
            playerPlayback(paused = false, awaitingPlayback = false, playbackConfirmed = true, offline = false),
        )
    }

    @Test
    fun anOfflineChannelIsOffline() {
        assertEquals(
            PlayerPlayback.Offline,
            playerPlayback(paused = false, awaitingPlayback = false, playbackConfirmed = false, offline = true),
        )
    }

    @Test
    fun anOfflineChannelIsOfflineEvenWhenTheViewerPausedItWhileItLoaded() {
        assertEquals(
            PlayerPlayback.Offline,
            playerPlayback(paused = true, awaitingPlayback = false, playbackConfirmed = false, offline = true),
        )
    }

    @Test
    fun aFullPlayerWithTheSettingsMenuShutLeavesItsMiddleFree() {
        assertTrue(playerCenterFree(compactSettled = false, progress = 0f, settingsMenuOpen = false))
    }

    @Test
    fun aPlayerOnItsWayToTheCornerTakesItsMiddle() {
        assertFalse(playerCenterFree(compactSettled = false, progress = 0.05f, settingsMenuOpen = false))
    }

    @Test
    fun theMiniPlayerTakesItsMiddle() {
        assertFalse(playerCenterFree(compactSettled = true, progress = 1f, settingsMenuOpen = false))
    }

    @Test
    fun theOpenSettingsMenuTakesTheMiddle() {
        assertFalse(playerCenterFree(compactSettled = false, progress = 0f, settingsMenuOpen = true))
    }

    @Test
    fun aPausedPlayerShowsPlayWithTheControlsUp() {
        assertEquals(
            PlayerCenterButton.Play,
            playerCenterButton(PlayerPlayback.Paused, controlsUp = true, centerFree = true),
        )
    }

    @Test
    fun aPausedPlayerShowsPlayWithTheControlsHidden() {
        assertEquals(
            PlayerCenterButton.Play,
            playerCenterButton(PlayerPlayback.Paused, controlsUp = false, centerFree = true),
        )
    }

    @Test
    fun aPlayingPlayerShowsPauseWithTheControlsUp() {
        assertEquals(
            PlayerCenterButton.Pause,
            playerCenterButton(PlayerPlayback.Playing, controlsUp = true, centerFree = true),
        )
    }

    @Test
    fun aPlayingPlayerShowsNothingWithTheControlsHidden() {
        assertNull(playerCenterButton(PlayerPlayback.Playing, controlsUp = false, centerFree = true))
    }

    @Test
    fun aStartingPlayerShowsNothingWithTheControlsUp() {
        assertNull(playerCenterButton(PlayerPlayback.Starting, controlsUp = true, centerFree = true))
    }

    @Test
    fun anOfflinePlayerShowsNothingWithTheControlsUp() {
        assertNull(playerCenterButton(PlayerPlayback.Offline, controlsUp = true, centerFree = true))
    }

    @Test
    fun anOfflinePlayerShowsNothingWithTheControlsHidden() {
        assertNull(playerCenterButton(PlayerPlayback.Offline, controlsUp = false, centerFree = true))
    }

    @Test
    fun aPausedPlayerShowsNothingWhileItsMiddleIsTaken() {
        assertNull(playerCenterButton(PlayerPlayback.Paused, controlsUp = true, centerFree = false))
    }

    @Test
    fun aPlayingPlayerShowsNothingWhileItsMiddleIsTaken() {
        assertNull(playerCenterButton(PlayerPlayback.Playing, controlsUp = true, centerFree = false))
    }
}
