package name.alexwayfer.customtv.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

object StreamPlayback {
    @Volatile
    var userPaused: Boolean = false
        private set
    @Volatile
    var isExplicitlyPaused: Boolean = false
        private set

    @Volatile
    private var awaitingPlayback: Boolean = false
    @Volatile
    var isPlaybackConfirmed: Boolean = false
        private set
    @Volatile
    private var playbackDisabled: Boolean = false
    private var playRequestedAt = 0L
    private val position = ReportedPosition()
    private val inferredPause = InferredPause(Handler(Looper.getMainLooper()))

    private var commands: Commands? = null

    class Commands(
        val play: () -> Unit,
        val pause: () -> Unit,
        val seek: (seconds: Double) -> Unit,
        val close: () -> Unit,
    )

    val isBound: Boolean
        get() = commands != null

    val isBuffering: Boolean
        get() = isBound && !userPaused && awaitingPlayback

    val positionMs: Long
        get() = position.currentMs(advancing = isPlaybackConfirmed && !userPaused && !awaitingPlayback)

    /** A playing report changes what the app shows: it was paused, waiting, or not confirmed yet. */
    private val playingChangesState: Boolean
        get() = userPaused || awaitingPlayback || !isPlaybackConfirmed

    /** A paused report now is not the viewer's pause: it is held off, already known, pending, or still loading. */
    private val pauseReportIgnored: Boolean
        get() = inferredPause.suppressed || userPaused || inferredPause.isPending ||
            awaitingPlayback && SystemClock.elapsedRealtime() - playRequestedAt < LOAD_GRACE_MS

    fun bind(commands: Commands) {
        this.commands = commands
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = false
        isPlaybackConfirmed = false
        playbackDisabled = false
        position.set(0L)
        inferredPause.cancel()
        PlayerPlaybackService.sync()
    }

    fun prepareForChannel() {
        inferredPause.cancel()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = true
        isPlaybackConfirmed = false
        playbackDisabled = false
        playRequestedAt = SystemClock.elapsedRealtime()
        position.set(0L)
        PlayerPlaybackService.sync()
    }

    /** The recording's time as the page reports it while it plays. */
    fun reportPosition(seconds: Double) {
        position.setSeconds(seconds)
    }

    /** The recording jumped to [seconds], by its own seek bar or by [seekTo]. */
    fun reportSeek(seconds: Double) {
        position.setSeconds(seconds)
        PlayerPlaybackService.sync()
    }

    fun seekTo(positionMs: Long) {
        position.set(positionMs)
        commands?.seek?.invoke(ReportedPosition.seconds(positionMs))
        PlayerPlaybackService.sync()
    }

    fun unbind(commands: Commands) {
        if (this.commands === commands) {
            this.commands = null
            inferredPause.cancel()
        }
    }

    fun play() {
        inferredPause.cancel()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = true
        playRequestedAt = SystemClock.elapsedRealtime()
        commands?.play?.invoke()
        PlayerPlaybackService.sync()
    }

    fun pause() {
        inferredPause.cancel()
        userPaused = true
        isExplicitlyPaused = true
        awaitingPlayback = false
        commands?.pause?.invoke()
        PlayerPlaybackService.sync()
    }

    fun stopForDismissal() {
        inferredPause.cancel()
        playbackDisabled = true
        userPaused = true
        isExplicitlyPaused = true
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
            inferredPause.cancel()
            val changed = playingChangesState
            userPaused = false
            isExplicitlyPaused = false
            awaitingPlayback = false
            isPlaybackConfirmed = true
            if (changed) PlayerPlaybackService.sync()
            return
        }
        if (pauseReportIgnored) return
        inferredPause.schedule(REBUFFER_GRACE_MS) {
            if (inferredPause.suppressed || userPaused || commands == null) return@schedule
            userPaused = true
            awaitingPlayback = false
            PlayerPlaybackService.sync()
        }
    }

    fun setSuppressInferredPause(suppress: Boolean) {
        inferredPause.suppress(suppress)
    }

    private const val LOAD_GRACE_MS = 12_000L
    private const val REBUFFER_GRACE_MS = 2_000L
}
