package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.JavascriptInterface

private const val LIVE_TICK_AD_BREAK = 1
private const val LIVE_TICK_WAITING_FOR_PLAYLIST_WITH_LOW_BUFFER = 2
private const val LIVE_TICK_HAS_PLAYBACK_BUFFER = 4
private const val LIVE_TICK_CAN_WAIT_FOR_SEGMENT_RETRY = 8
private const val LIVE_TICK_SEGMENT_RETRY_DEADLINE = 16
private const val MAX_MEDIA_REQUEST_ID_LENGTH = 32

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
    private val playlistRetryAllowed: () -> Boolean,
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
    fun onLiveTick(
        positionSeconds: Double,
        latencySeconds: Double,
        state: String?,
        buffered: String?,
        status: String?,
    ): Boolean {
        if (!positionSeconds.isFinite() || positionSeconds < 0.0) return false
        // Keep the optional request ID with the flags from the same sample.
        val flags = status?.substringBefore(':')?.toIntOrNull() ?: return false
        val retryId = status.substringAfter(':', "")
            .takeIf { it.isNotEmpty() && it.length <= MAX_MEDIA_REQUEST_ID_LENGTH }
        val tick = LiveTick(
            positionSeconds = positionSeconds,
            latencySeconds = latencySeconds.takeIf { it.isFinite() && it >= 0.0 } ?: Double.NaN,
            state = state.orEmpty(),
            adBreak = flags and LIVE_TICK_AD_BREAK != 0,
            buffered = buffered.orEmpty(),
            waitingForPlaylistWithLowBuffer = flags and LIVE_TICK_WAITING_FOR_PLAYLIST_WITH_LOW_BUFFER != 0,
            hasPlaybackBuffer = flags and LIVE_TICK_HAS_PLAYBACK_BUFFER != 0,
            segmentRetry = LiveSegmentRetry(
                id = retryId,
                canWait = flags and LIVE_TICK_CAN_WAIT_FOR_SEGMENT_RETRY != 0,
                deadline = flags and LIVE_TICK_SEGMENT_RETRY_DEADLINE != 0,
            ),
        )
        post { events.onLiveTick(tick) }
        return playlistRetryAllowed()
    }
}
