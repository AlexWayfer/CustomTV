package name.alexwayfer.customtv.player

import android.os.SystemClock
import name.alexwayfer.customtv.diagnostics.AppLog

/** Runs what [LiveStallWatchdog] decides on the live page's reports, and logs each step with what the video showed. */
internal class LiveStallRecovery(
    private val isLive: () -> Boolean,
    private val isPlaybackConfirmed: () -> Boolean,
    private val restartPlayer: () -> Unit,
    private val reloadPage: () -> Unit,
) {
    private val watchdog = LiveStallWatchdog()

    fun tick(tick: LiveTick) {
        val watching = LiveStallWatchdog.watches(
            live = isLive(),
            playbackConfirmed = isPlaybackConfirmed(),
            userPaused = StreamPlayback.userPaused,
            tick = tick,
        )
        val action = watchdog.tick(SystemClock.elapsedRealtime(), watching, tick.positionSeconds, tick.latencySeconds)
        if (action == LiveStallAction.None) return
        val step = when (action) {
            LiveStallAction.RestartPlayer -> "restarting the player"
            LiveStallAction.ReloadPage -> "reloading the page"
            else -> "no steps left"
        }
        AppLog.w(
            PLAYER_LOG_TAG,
            "Live stuck at ${tick.positionSeconds} s, ${tick.latencySeconds} s behind, ${tick.state}, " +
                "buffered ${tick.buffered}: $step",
        )
        when (action) {
            LiveStallAction.RestartPlayer -> restartPlayer()
            LiveStallAction.ReloadPage -> reloadPage()
            else -> Unit
        }
    }

    fun reset() = watchdog.reset()
}
