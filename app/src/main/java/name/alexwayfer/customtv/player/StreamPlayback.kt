package name.alexwayfer.customtv.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val pauseConfirmation = PauseConfirmation()

    private var commands: Commands? = null

    private val mutablePlayback = MutableStateFlow(PlayerPlayback.Starting)

    /** What the screen shows of the playback, such as Play while the viewer paused. */
    internal val playback: StateFlow<PlayerPlayback> = mutablePlayback.asStateFlow()

    class Commands(
        val play: () -> Unit,
        /** Pauses the page and calls back once the page has run the pause. */
        val pause: (onRun: () -> Unit) -> Unit,
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
        pauseConfirmation.clear()
        StreamInterruption.clear()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = false
        isPlaybackConfirmed = false
        playbackDisabled = false
        position.set(0L)
        inferredPause.cancel()
        syncState()
    }

    fun prepareForChannel() {
        inferredPause.cancel()
        pauseConfirmation.clear()
        StreamInterruption.clear()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = true
        isPlaybackConfirmed = false
        playbackDisabled = false
        playRequestedAt = SystemClock.elapsedRealtime()
        position.set(0L)
        syncState()
    }

    /** The recording's time as the page reports it while it plays. */
    fun reportPosition(seconds: Double) {
        position.setSeconds(seconds)
    }

    /** The recording jumped to [seconds], by its own seek bar or by [seekTo]. */
    fun reportSeek(seconds: Double) {
        position.setSeconds(seconds)
        syncState()
    }

    fun seekTo(positionMs: Long) {
        position.set(positionMs)
        commands?.seek?.invoke(ReportedPosition.seconds(positionMs))
        syncState()
    }

    fun unbind(commands: Commands) {
        if (this.commands === commands) {
            this.commands = null
            inferredPause.cancel()
        }
    }

    fun play() {
        inferredPause.cancel()
        pauseConfirmation.clear()
        StreamInterruption.clear()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = true
        playRequestedAt = SystemClock.elapsedRealtime()
        commands?.play?.invoke()
        syncState()
    }

    fun pause() {
        inferredPause.cancel()
        pauseConfirmation.sent(loading = !isPlaybackConfirmed)
        StreamInterruption.clear()
        userPaused = true
        isExplicitlyPaused = true
        awaitingPlayback = false
        commands?.pause?.invoke(pauseConfirmation::ran)
        syncState()
    }

    fun stopForDismissal() {
        inferredPause.cancel()
        StreamInterruption.clear()
        playbackDisabled = true
        userPaused = true
        isExplicitlyPaused = true
        awaitingPlayback = false
        commands?.pause?.invoke {}
        commands?.close?.invoke()
        syncState()
    }

    fun reportActualState(playing: Boolean) {
        if (playbackDisabled) {
            if (playing) commands?.pause?.invoke {}
            return
        }
        // A frame the page reported before it ran the app's pause does not undo the pause.
        if (pauseConfirmation.stale(playing)) return
        // Twitch started the stream the viewer paused while it loaded: it stays paused.
        if (pauseConfirmation.startedOnItsOwn(playing)) {
            pauseConfirmation.sent(loading = false)
            commands?.pause?.invoke(pauseConfirmation::ran)
            return
        }
        if (playing) {
            inferredPause.cancel()
            StreamInterruption.clear()
            val changed = playingChangesState
            userPaused = false
            isExplicitlyPaused = false
            awaitingPlayback = false
            isPlaybackConfirmed = true
            if (changed) syncState()
            return
        }
        if (pauseReportIgnored) return
        // Twitch's player paused without the app asking, as it may when another app takes the sound.
        StreamInterruption.report()
        inferredPause.schedule(REBUFFER_GRACE_MS) {
            if (inferredPause.suppressed || userPaused || commands == null) return@schedule
            userPaused = true
            awaitingPlayback = false
            syncState()
        }
    }

    var inferredPauseSuppressed: Boolean
        get() = inferredPause.suppressed
        set(value) = inferredPause.suppress(value)

    /** Tells the media session and the screen what changed. */
    private fun syncState() {
        mutablePlayback.value = playerPlayback(userPaused, awaitingPlayback, isPlaybackConfirmed)
        PlayerPlaybackService.sync()
    }

    private const val LOAD_GRACE_MS = 12_000L
    private const val REBUFFER_GRACE_MS = 2_000L
}
