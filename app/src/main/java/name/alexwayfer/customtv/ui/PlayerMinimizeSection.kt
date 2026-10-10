package name.alexwayfer.customtv.ui

/**
 * The section under the mini player. A stream opened from a profile minimizes back onto that
 * profile; a full stream opened elsewhere minimizes onto Home.
 */
internal fun sectionAfterPlayerMinimizes(
    section: AppSection,
    watching: Boolean,
    minimized: Boolean,
): AppSection = when {
    sectionStaysUnderPlayer(section) -> section
    section == AppSection.Home || (watching && !minimized) -> AppSection.Home
    else -> section
}

/**
 * The mini player would cover the app settings and the whisper conversations, so it steps off
 * screen there. Playback and chat keep running; a full player and picture-in-picture stay on screen.
 */
internal fun miniPlayerHidden(section: AppSection, minimized: Boolean, inPictureInPicture: Boolean): Boolean =
    minimized && !inPictureInPicture && (section == AppSection.Settings || section == AppSection.Whispers)

/**
 * Back on Home with a mini player leaves the app into picture-in-picture, so the player keeps
 * playing. Other sections handle Back themselves first.
 */
internal fun homeBackKeepsMiniPlayer(
    section: AppSection,
    liveMiniPlayer: Boolean,
    videoMiniPlayer: Boolean,
    inPictureInPicture: Boolean,
): Boolean = section == AppSection.Home && !inPictureInPicture && (liveMiniPlayer || videoMiniPlayer)

/** Picture-in-picture is offered for a stream that is live and not dismissed, and for an open recording. */
internal fun pictureInPictureAllowed(
    watching: Boolean,
    channelLive: Boolean,
    liveClosedWithPip: Boolean,
    recordingOpen: Boolean,
): Boolean = (watching && channelLive && !liveClosedWithPip) || recordingOpen
