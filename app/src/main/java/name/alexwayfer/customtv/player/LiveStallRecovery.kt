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
    // The state of Twitch's player last logged during a recovery, so each change shows once.
    private var recoveryState: String? = null

    fun tick(tick: LiveTick) {
        val watch = LiveStallWatchdog.watch(
            live = isLive(),
            playbackConfirmed = isPlaybackConfirmed(),
            userPaused = StreamPlayback.userPaused,
            tick = tick,
        )
        val action = watchdog.tick(SystemClock.elapsedRealtime(), watch, tick.positionSeconds, tick.latencySeconds)
        logRecoveryState(tick)
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

    fun reset() {
        watchdog.reset()
        recoveryState = null
    }

    private fun logRecoveryState(tick: LiveTick) {
        if (!watchdog.recovering) {
            if (recoveryState != null) {
                AppLog.i(PLAYER_LOG_TAG, "Live recovery over at ${tick.positionSeconds} s, ${tick.state}")
            }
            recoveryState = null
            return
        }
        if (tick.state == recoveryState) return
        recoveryState = tick.state
        AppLog.i(
            PLAYER_LOG_TAG,
            "Live recovering at ${tick.positionSeconds} s, ${tick.state}, buffered ${tick.buffered}",
        )
    }
}
