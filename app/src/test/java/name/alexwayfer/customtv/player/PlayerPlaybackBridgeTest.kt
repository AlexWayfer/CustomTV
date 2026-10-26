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
    private val states = mutableListOf<Boolean>()
    private val positions = mutableListOf<Double>()
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
            onLiveTick = {},
        ),
        isReleased = { released },
    )

    @Test
    fun reportsOfAPlayerOnScreenArrive() {
        bridge.onState(true)
        bridge.onPosition(12.0)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(true), states)
        assertEquals(listOf(12.0), positions)
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
    fun aReportSentJustBeforeTheReleaseIsDroppedWhenItRuns() {
        bridge.onState(false)
        released = true
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(emptyList<Boolean>(), states)
    }
}
