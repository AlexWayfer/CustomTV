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
    copy(liveMinimized = false, liveClosedWithPip = false, video = null, videoMinimized = false)

/** A recording opens full screen; the stream keeps [liveMinimized] from the open decision. */
internal fun PlayerSession.videoOpened(
    video: ProfileVideoPlayback,
    liveMinimized: Boolean,
): PlayerSession = copy(liveMinimized = liveMinimized, video = video, videoMinimized = false)

internal fun PlayerSession.videoClosed(): PlayerSession = copy(video = null, videoMinimized = false)

/** The sleep timer closes whichever player is open, full screen or minimized; the caller pops the streams. */
internal fun PlayerSession.sleepTimerFired(): PlayerSession =
    copy(liveMinimized = false, video = null, videoMinimized = false)

internal fun PlayerSession.withLiveMinimized(minimized: Boolean): PlayerSession =
    copy(liveMinimized = minimized)

internal fun PlayerSession.withVideoMinimized(minimized: Boolean): PlayerSession =
    copy(videoMinimized = minimized)
