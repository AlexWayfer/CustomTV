package name.alexwayfer.customtv.player

import android.content.Context
import android.media.AudioManager
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
    /** The channel is offline: its paused video is not the viewer's pause, until a frame plays or a new load. */
    @Volatile
    private var offline: Boolean = false
    private var playRequestedAt = 0L
    private val position = ReportedPosition()
    private val inferredPause = InferredPause(Handler(Looper.getMainLooper()))
    private val pauseConfirmation = PauseConfirmation()

    private var commands: Commands? = null
    private var audioManager: AudioManager? = null

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

    /** Automatic playback respects viewer pauses and audio interruptions. */
    internal val canAutomaticallyPlay: Boolean
        get() = canAutomaticallyPlayIgnoringUnattributedAudio && !interruptedDuringPlayback

    internal val canAutomaticallyPlayIgnoringUnattributedAudio: Boolean
        get() = !userPaused && !StreamInterruption.microphoneHoldsPlayback

    // Android's list includes the stream's own audio. Check it only after the page reports an interruption.
    private val interruptedDuringPlayback: Boolean
        get() = StreamInterruption.interrupted.value && audioManager?.activePlaybackConfigurations?.isNotEmpty() == true

    val isBuffering: Boolean
        get() = isBound && !userPaused && awaitingPlayback

    val positionMs: Long
        get() = position.currentMs(advancing = isPlaybackConfirmed && !userPaused && !awaitingPlayback)

    /** A playing report changes what the app shows: it was paused, waiting, or not confirmed yet. */
    private val playingChangesState: Boolean
        get() = userPaused || awaitingPlayback || !isPlaybackConfirmed || offline

    /** Playback started, and neither the viewer nor the offline channel asked it to stop. */
    private val expectsPlayback: Boolean
        get() = isPlaybackConfirmed && !userPaused && !offline

    /**
     * A paused report now is not the viewer's pause: it is held off, already known, pending, still loading, or of an
     * offline channel.
     */
    private val pauseReportIgnored: Boolean
        get() = offline || inferredPause.suppressed || userPaused || inferredPause.isPending ||
            awaitingPlayback && SystemClock.elapsedRealtime() - playRequestedAt < LOAD_GRACE_MS

    fun bind(commands: Commands, context: Context) {
        this.commands = commands
        audioManager = context.getSystemService(AudioManager::class.java)
        pauseConfirmation.clear()
        StreamInterruption.clear()
        userPaused = false
        isExplicitlyPaused = false
        awaitingPlayback = false
        isPlaybackConfirmed = false
        playbackDisabled = false
        offline = false
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
        offline = false
        playRequestedAt = SystemClock.elapsedRealtime()
        position.set(0L)
        syncState()
    }

    /** The page shows the channel offline: nothing plays, and Play would have nothing to start. */
    var channelOffline: Boolean
        get() = offline
        set(value) {
            if (offline == value) return
            inferredPause.cancel()
            StreamInterruption.clear()
            offline = value
            if (value) awaitingPlayback = false
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
            audioManager = null
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

    fun pause(holdUntilPlay: Boolean = false) {
        inferredPause.cancel()
        pauseConfirmation.sent(loading = !isPlaybackConfirmed, holdUntilPlay = holdUntilPlay)
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
        val report = pauseConfirmation.report(playing)
        when {
            report == PauseReport.Retry -> commands?.pause?.invoke(pauseConfirmation::ran)
            report == PauseReport.Ignore -> Unit
            playing -> {
                inferredPause.cancel()
                StreamInterruption.clear()
                val changed = playingChangesState
                userPaused = false
                isExplicitlyPaused = false
                awaitingPlayback = false
                isPlaybackConfirmed = true
                offline = false
                if (changed) syncState()
            }
            else -> {
                // Background pauses remain recoverable, but microphone recording must remember this interruption too.
                if (inferredPause.suppressed && expectsPlayback) {
                    StreamInterruption.report()
                }
                if (pauseReportIgnored) return
                // Twitch may pause without the app asking, such as when another app takes the sound.
                StreamInterruption.report()
                inferredPause.schedule(REBUFFER_GRACE_MS) {
                    if (inferredPause.suppressed || userPaused || commands == null) return@schedule
                    userPaused = true
                    awaitingPlayback = false
                    syncState()
                }
            }
        }
    }

    var inferredPauseSuppressed: Boolean
        get() = inferredPause.suppressed
        set(value) = inferredPause.suppress(value)

    /** Tells the media session and the screen what changed. */
    private fun syncState() {
        mutablePlayback.value = playerPlayback(userPaused, awaitingPlayback, isPlaybackConfirmed, offline)
        PlayerPlaybackService.sync()
    }

    private const val LOAD_GRACE_MS = 12_000L
    private const val REBUFFER_GRACE_MS = 2_000L
}
