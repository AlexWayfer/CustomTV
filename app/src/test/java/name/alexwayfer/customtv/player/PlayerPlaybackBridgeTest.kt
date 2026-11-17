package name.alexwayfer.customtv.player

import android.app.Application
import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerPlaybackBridgeTest {
    private var released = false
    private var currentPage = true
    private var playlistRetryAllowed = false
    private val states = mutableListOf<Boolean>()
    private val positions = mutableListOf<Double>()
    private val liveTicks = mutableListOf<LiveTick>()
    private val bridge = PlayerPlaybackBridge(
        handler = Handler(Looper.getMainLooper()),
        events = PlayerPlaybackEvents(
            onPlaybackState = { states += it },
            onInterrupted = {},
            onPositionSeconds = { positions += it },
            onSeekSeconds = {},
            onPlayerReady = {},
            onOffline = {},
            onAudienceGate = {},
            onPlayerError = {},
            onPlayerReload = {},
            onLiveTick = { liveTicks += it },
        ),
        isReleased = { released },
        isCurrentPage = { currentPage },
        playlistRetryAllowed = { playlistRetryAllowed },
    )

    @Test
    fun thePageReadsTheCurrentPlaylistRetryPermission() {
        assertEquals(false, bridge.onLiveTick(12.0, 3.0, "Buffering", "", status = "0"))
        playlistRetryAllowed = true
        assertEquals(true, bridge.onLiveTick(12.0, 3.0, "Buffering", "", status = "0"))
    }

    @Test
    fun reportsOfAPlayerOnScreenArrive() {
        bridge.onState(true)
        bridge.onPosition(12.0)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(true), states)
        assertEquals(listOf(12.0), positions)
    }

    @Test
    fun liveTickKeepsThePlaybackAndNetworkFlagsIndependent() {
        bridge.onLiveTick(12.0, 3.0, "Buffering", "[0.0, 12.0]", status = "1")
        bridge.onLiveTick(12.0, 3.0, "Buffering", "[0.0, 12.0]", status = "2")
        bridge.onLiveTick(12.0, 3.0, "Playing", "[0.0, 24.0]", status = "4")
        bridge.onLiveTick(12.0, 3.0, "Buffering", "[0.0, 12.0]", status = "8:worker:2")
        bridge.onLiveTick(12.0, 3.0, "Buffering", "[0.0, 12.0]", status = "16")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(true, false, false, false, false), liveTicks.map { it.adBreak })
        assertEquals(listOf(false, true, false, false, false), liveTicks.map { it.waitingForPlaylistWithLowBuffer })
        assertEquals(listOf(false, false, true, false, false), liveTicks.map { it.hasPlaybackBuffer })
        assertEquals(LiveSegmentRetry("worker:2", canWait = true), liveTicks[3].segmentRetry)
        assertEquals(LiveSegmentRetry(null, canWait = false, deadline = true), liveTicks[4].segmentRetry)
    }

    @Test
    fun reportsOfAReleasedPlayerFadingOutAreDropped() {
        released = true
        bridge.onState(false)
        bridge.onPosition(12.0)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(emptyList<Boolean>(), states)
        assertEquals(emptyList<Double>(), positions)
    }

    @Test
    fun reportsOfThePageANewLoadReplacesAreDropped() {
        currentPage = false
        bridge.onState(true)
        bridge.onPosition(12.0)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(emptyList<Boolean>(), states)
        assertEquals(emptyList<Double>(), positions)
    }

    @Test
    fun aReportSentJustBeforeTheReleaseIsDroppedWhenItRuns() {
        bridge.onState(false)
        released = true
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(emptyList<Boolean>(), states)
    }
}
