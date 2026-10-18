package name.alexwayfer.customtv.ui

import androidx.compose.runtime.saveable.Saver
import name.alexwayfer.customtv.ui.account.ProfileVideoPlayback

/**
 * What the players show. Which stream is open lives in the navigation back stack; this holds the
 * rest. Only one player is on screen: opening a stream closes the recording, and opening a
 * recording goes through [profileVideoOpenDecision], which closes the stream.
 */
internal data class PlayerSession(
    val liveMinimized: Boolean = false,
    val liveClosedWithPip: Boolean = false,
    val video: ProfileVideoPlayback? = null,
    val videoMinimized: Boolean = false,
    /** The stream Back on the full [video] opens again, from [linkedVideoReturnChannel]; null where Back minimizes. */
    val videoReturnChannel: String? = null,
) {
    companion object {
        /** Keeps the stream's minimized state across process death, as before; a recording reopens by hand. */
        val Saver: Saver<PlayerSession, Boolean> = Saver(
            save = { it.liveMinimized },
            restore = { PlayerSession(liveMinimized = it) },
        )
    }
}

/** A stream opens full screen and closes the recording. */
internal fun PlayerSession.liveOpened(): PlayerSession =
    copy(
        liveMinimized = false,
        liveClosedWithPip = false,
        video = null,
        videoMinimized = false,
        videoReturnChannel = null,
    )

/**
 * A recording opens full screen; the stream keeps [liveMinimized] from the open decision, and Back on the recording
 * opens [returnChannel] again when there is one.
 */
internal fun PlayerSession.videoOpened(
    video: ProfileVideoPlayback,
    liveMinimized: Boolean,
    returnChannel: String? = null,
): PlayerSession =
    copy(liveMinimized = liveMinimized, video = video, videoMinimized = false, videoReturnChannel = returnChannel)

internal fun PlayerSession.videoClosed(): PlayerSession =
    copy(video = null, videoMinimized = false, videoReturnChannel = null)

/**
 * The stream Back opens again from the recording: only from the full player, since Back on the mini player keeps
 * doing what it does for every recording.
 */
internal val PlayerSession.videoBackReturnChannel: String?
    get() = videoReturnChannel?.takeIf { video != null && !videoMinimized }

/** The sleep timer closes whichever player is open, full screen or minimized; the caller pops the streams. */
internal fun PlayerSession.sleepTimerFired(): PlayerSession =
    copy(liveMinimized = false, video = null, videoMinimized = false, videoReturnChannel = null)

internal fun PlayerSession.withLiveMinimized(minimized: Boolean): PlayerSession =
    copy(liveMinimized = minimized)

internal fun PlayerSession.withVideoMinimized(minimized: Boolean): PlayerSession =
    copy(videoMinimized = minimized)
