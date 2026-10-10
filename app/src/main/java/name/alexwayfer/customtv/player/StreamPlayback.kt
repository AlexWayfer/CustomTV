package name.alexwayfer.customtv.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

object StreamPlayback {
    @Volatile
    var userPaused: Boolean = false
        private set
    @Volatile
    private var explicitlyPaused: Boolean = false

    @Volatile
    private var awaitingPlayback: Boolean = false
    @Volatile
    private var playbackConfirmed: Boolean = false
    @Volatile
    private var playbackDisabled: Boolean = false
    @Volatile
    private var suppressInferredPause: Boolean = false
    private var playRequestedAt = 0L
    @Volatile
    private var reportedPositionMs = 0L
    @Volatile
    private var positionReportedAt = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingPause: Runnable? = null

    private var commands: Commands? = null

    class Commands(
        val play: () -> Unit,
        val pause: () -> Unit,
        val seek: (seconds: Double) -> Unit,
        val close: () -> Unit,
    )

    fun bind(commands: Commands) {
        this.commands = commands
        userPaused = false
        explicitlyPaused = false
        awaitingPlayback = false
        playbackConfirmed = false
        playbackDisabled = false
        setPosition(0L)
        cancelPendingPause()
        PlayerPlaybackService.sync()
    }

    fun prepareForChannel() {
        cancelPendingPause()
        userPaused = false
        explicitlyPaused = false
        awaitingPlayback = true
        playbackConfirmed = false
        playbackDisabled = false
        playRequestedAt = SystemClock.elapsedRealtime()
        setPosition(0L)
        PlayerPlaybackService.sync()
    }

    /** The recording's time as the page reports it while it plays. */
    fun reportPosition(seconds: Double) {
        setPosition((seconds * 1_000).toLong())
    }

    /** The recording jumped to [seconds], by its own seek bar or by [seekTo]. */
    fun reportSeek(seconds: Double) {
        setPosition((seconds * 1_000).toLong())
        PlayerPlaybackService.sync()
    }

    fun seekTo(positionMs: Long) {
        setPosition(positionMs)
        commands?.seek?.invoke(positionMs / 1_000.0)
        PlayerPlaybackService.sync()
    }

    fun positionMs(): Long = playbackPositionMs(
        reportedMs = reportedPositionMs,
        reportedAtMs = positionReportedAt,
        nowMs = SystemClock.elapsedRealtime(),
        advancing = playbackConfirmed && !userPaused && !awaitingPlayback,
    )

    private fun setPosition(positionMs: Long) {
        reportedPositionMs = positionMs.coerceAtLeast(0L)
        positionReportedAt = SystemClock.elapsedRealtime()
    }

    fun unbind(commands: Commands) {
        if (this.commands === commands) {
            this.commands = null
            cancelPendingPause()
        }
    }

    fun play() {
        cancelPendingPause()
        userPaused = false
        explicitlyPaused = false
        awaitingPlayback = true
        playRequestedAt = SystemClock.elapsedRealtime()
        commands?.play?.invoke()
        PlayerPlaybackService.sync()
    }

    fun pause() {
        cancelPendingPause()
        userPaused = true
        explicitlyPaused = true
        awaitingPlayback = false
        commands?.pause?.invoke()
        PlayerPlaybackService.sync()
    }

    fun stopForDismissal() {
        cancelPendingPause()
        playbackDisabled = true
        userPaused = true
        explicitlyPaused = true
        awaitingPlayback = false
        commands?.pause?.invoke()
        commands?.close?.invoke()
        PlayerPlaybackService.sync()
    }

    fun reportActualState(playing: Boolean) {
        if (playbackDisabled) {
            if (playing) commands?.pause?.invoke()
            return
        }
        if (playing) {
            cancelPendingPause()
            val changed = userPaused || awaitingPlayback || !playbackConfirmed
            userPaused = false
            explicitlyPaused = false
            awaitingPlayback = false
            playbackConfirmed = true
            if (changed) PlayerPlaybackService.sync()
            return
        }
        if (suppressInferredPause || userPaused || pendingPause != null) return
        if (awaitingPlayback && SystemClock.elapsedRealtime() - playRequestedAt < LOAD_GRACE_MS) {
            return
        }
        val task = Runnable {
            pendingPause = null
            if (suppressInferredPause || userPaused || commands == null) return@Runnable
            userPaused = true
            awaitingPlayback = false
            PlayerPlaybackService.sync()
        }
        pendingPause = task
        mainHandler.postDelayed(task, REBUFFER_GRACE_MS)
    }

    fun setSuppressInferredPause(suppress: Boolean) {
        suppressInferredPause = suppress
        if (suppress) cancelPendingPause()
    }

    fun isBound(): Boolean = commands != null

    fun isPlaybackConfirmed(): Boolean = playbackConfirmed

    fun isBuffering(): Boolean = isBound() && !userPaused && awaitingPlayback

    fun isExplicitlyPaused(): Boolean = explicitlyPaused

    private fun cancelPendingPause() {
        pendingPause?.let { mainHandler.removeCallbacks(it) }
        pendingPause = null
    }

    private const val LOAD_GRACE_MS = 12_000L
    private const val REBUFFER_GRACE_MS = 2_000L
}
