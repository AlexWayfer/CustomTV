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
    val onLiveTick: (LiveTick) -> Unit,
)

/**
 * Hands [PlayerPlaybackEvents] to the page as `CustomTvPlayback`. A released player's page still runs while its sound
 * fades out, and its reports would land on the playback of the player that took over, so they are dropped. So are
 * the reports of a page that a new load replaces, until the new page starts.
 */
internal class PlayerPlaybackBridge(
    private val handler: Handler,
    private val events: PlayerPlaybackEvents,
    private val isReleased: () -> Boolean,
    private val isCurrentPage: () -> Boolean,
) {
    private fun post(event: () -> Unit) {
        handler.post { if (!isReleased() && isCurrentPage()) event() }
    }

    @JavascriptInterface
    fun onState(playing: Boolean) {
        post { events.onPlaybackState(playing) }
    }

    /** The video stopped while Twitch's player still means to play it. */
    @JavascriptInterface
    @Suppress("unused")
    fun onInterrupted() {
        post(events.onInterrupted)
    }

    @JavascriptInterface
    fun onPosition(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            post { events.onPositionSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onSeek(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            post { events.onSeekSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReady() {
        post(events.onPlayerReady)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onOffline() {
        post(events.onOffline)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onAudienceGate() {
        post(events.onAudienceGate)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerError() {
        post(events.onPlayerError)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReload() {
        post(events.onPlayerReload)
    }

    /** A live page's tick: the video's position and buffer, and the latency and state of Twitch's player. */
    @JavascriptInterface
    @Suppress("unused")
    fun onLiveTick(
        positionSeconds: Double,
        latencySeconds: Double,
        state: String?,
        adBreak: Boolean,
        buffered: String?,
    ) {
        if (!positionSeconds.isFinite() || positionSeconds < 0.0) return
        val tick = LiveTick(
            positionSeconds = positionSeconds,
            latencySeconds = latencySeconds.takeIf { it.isFinite() && it >= 0.0 } ?: Double.NaN,
            state = state.orEmpty(),
            adBreak = adBreak,
            buffered = buffered.orEmpty(),
        )
        post { events.onLiveTick(tick) }
    }
}
