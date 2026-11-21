package name.alexwayfer.customtv.player

import android.app.Application
import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerPageRecoveryTest {
    private var pageReloads = 0
    private val mainLooper = Looper.getMainLooper()
    private val looper = shadowOf(mainLooper)
    private val recovery = PlayerPageRecovery(
        handler = Handler(mainLooper),
        isReleased = { false },
        isSurfaceRevealed = { false },
        actions = PlayerPageActions(
            reloadPlayer = {},
            reloadPage = { pageReloads += 1 },
            concealSurface = {},
            logPlayerState = { it() },
        ),
    )

    @Test
    fun anInferredPauseDoesNotStopRecoveryAfterTheErrorScreenReload() {
        val commands = StreamPlayback.Commands(play = {}, pause = { it() }, seek = {}, close = {})
        StreamPlayback.bind(commands, RuntimeEnvironment.getApplication())
        try {
            StreamPlayback.reportActualState(playing = true)
            recovery.errorScreenReload()
            StreamPlayback.reportActualState(playing = false)
            looper.idleFor(Duration.ofSeconds(10))

            assertTrue(StreamPlayback.userPaused)
            assertFalse(StreamPlayback.isExplicitlyPaused)
            assertEquals(1, pageReloads)
        } finally {
            StreamPlayback.unbind(commands)
            StreamPlayback.prepareForChannel()
        }
    }

    @Test
    fun aFailedErrorScreenReloadWaitsForTheNetwork() {
        recovery.networkAvailabilityChanged(available = false)
        recovery.errorScreenReload()
        looper.idleFor(Duration.ofSeconds(30))
        assertEquals(0, pageReloads)

        recovery.networkAvailabilityChanged(available = true)
        assertEquals(1, pageReloads)
        looper.idleFor(Duration.ofSeconds(20))
        assertEquals(1, pageReloads)
    }

    @Test
    fun returningOnlineBeforeTheTimeoutLetsTheFirstReloadFinish() {
        recovery.networkAvailabilityChanged(available = false)
        recovery.errorScreenReload()
        looper.idleFor(Duration.ofSeconds(5))
        recovery.networkAvailabilityChanged(available = true)
        assertEquals(0, pageReloads)

        recovery.resetAutomaticReloads()
        looper.idleFor(Duration.ofSeconds(10))
        assertEquals(0, pageReloads)
    }

    @Test
    fun aManualPauseWhileOfflinePreventsTheFallbackOnReconnect() {
        recovery.networkAvailabilityChanged(available = false)
        recovery.errorScreenReload()
        looper.idleFor(Duration.ofSeconds(10))
        StreamPlayback.pause()
        try {
            recovery.networkAvailabilityChanged(available = true)
            assertEquals(0, pageReloads)
        } finally {
            StreamPlayback.prepareForChannel()
        }
    }

    @Test
    fun aNewPageDoesNotInheritAReloadWaitingForTheNetwork() {
        recovery.networkAvailabilityChanged(available = false)
        recovery.errorScreenReload()
        looper.idleFor(Duration.ofSeconds(10))
        recovery.start(resetStartupReloads = true)

        recovery.networkAvailabilityChanged(available = true)
        assertEquals(0, pageReloads)
    }

    @Test
    fun aStreamThatRestartsAfterGoingOfflineConfirmsPlaybackAgain() {
        assertTrue(recovery.confirmPlayback())
        assertFalse(recovery.confirmPlayback())

        recovery.videoWithheld()

        assertFalse(recovery.playbackConfirmed)
        assertTrue(recovery.confirmPlayback())
        assertFalse(recovery.confirmPlayback())
    }
}
