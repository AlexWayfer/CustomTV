package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.JavascriptInterface

/**
 * What the player page reports: playback, a stop Twitch did not ask for, position and seeks, readiness, the
 * channel going offline, Twitch's audience gate waiting for a tap, and errors.
 */
internal data class PlayerPlaybackEvents(
    val onPlaybackState: (Boolean) -> Unit,
    val onInterrupted: () -> Unit,
    val onPositionSeconds: (Double) -> Unit,
    val onSeekSeconds: (Double) -> Unit,
    val onPlayerReady: () -> Unit,
    val onOffline: () -> Unit,
    val onAudienceGate: () -> Unit,
    val onPlayerError: () -> Unit,
    val onPlayerReload: () -> Unit,
)

internal class PlayerPlaybackBridge(
    private val handler: Handler,
    private val events: PlayerPlaybackEvents,
) {
    @JavascriptInterface
    @Suppress("unused")
    fun onState(playing: Boolean) {
        handler.post { events.onPlaybackState(playing) }
    }

    /** The video stopped while Twitch's player still means to play it. */
    @JavascriptInterface
    @Suppress("unused")
    fun onInterrupted() {
        handler.post(events.onInterrupted)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPosition(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            handler.post { events.onPositionSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onSeek(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            handler.post { events.onSeekSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReady() {
        handler.post(events.onPlayerReady)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onOffline() {
        handler.post(events.onOffline)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onAudienceGate() {
        handler.post(events.onAudienceGate)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerError() {
        handler.post(events.onPlayerError)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReload() {
        handler.post(events.onPlayerReload)
    }
}
