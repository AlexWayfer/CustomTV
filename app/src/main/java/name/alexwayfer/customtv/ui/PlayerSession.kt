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

/** Opening the Whispers section minimizes whichever player covers it. */
internal fun PlayerSession.whispersOpened(watching: Boolean): PlayerSession = coveringPlayerMinimized(watching)

/** A section opened from outside, such as from a notification, minimizes whichever player covers it. */
internal fun PlayerSession.coveringPlayerMinimized(watching: Boolean): PlayerSession = copy(
    liveMinimized = watching || liveMinimized,
    videoMinimized = video != null || videoMinimized,
)

/** Whether the player is full screen as the Whispers section opens, so Back from the section expands it again. */
internal fun PlayerSession.fullScreenBeforeWhispers(watching: Boolean): Boolean =
    (watching && !liveMinimized) || (video != null && !videoMinimized)

/** Back from the Whispers section shows a player full screen again when it was full screen before. */
internal fun PlayerSession.returnedFromWhispers(watching: Boolean): PlayerSession = copy(
    liveMinimized = !watching && liveMinimized,
    videoMinimized = video == null && videoMinimized,
)

/** Closing picture-in-picture stops the stream; returning to the app shows it again full screen. */
internal fun PlayerSession.livePipClosed(): PlayerSession =
    copy(liveClosedWithPip = true, liveMinimized = false)

internal fun PlayerSession.livePipResumed(): PlayerSession = copy(liveClosedWithPip = false)

/** Expanding picture-in-picture back into the app shows the player that was in it full screen. */
internal fun PlayerSession.pipExpanded(watching: Boolean): PlayerSession = copy(
    liveMinimized = !watching && liveMinimized,
    videoMinimized = video == null && videoMinimized,
)
