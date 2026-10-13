package name.alexwayfer.customtv.ui

/** Closing picture-in-picture stops the stream; returning to the app shows it again full screen. */
internal fun PlayerSession.livePipClosed(): PlayerSession =
    copy(liveClosedWithPip = true, liveMinimized = false)

internal fun PlayerSession.livePipResumed(): PlayerSession = copy(liveClosedWithPip = false)

/**
 * The player in picture-in-picture was minimized over [section], a section other than Home, so expanding it brings
 * that section back with the mini player, as it was left.
 */
internal fun PlayerSession.pipReturnsToSection(watching: Boolean, section: AppSection): Boolean =
    section != AppSection.Home && if (video != null) videoMinimized else watching && liveMinimized

/**
 * While picture-in-picture shows only the player, the section it returns to stays composed out of sight, so its open
 * page and what was typed there are still there on the way back.
 */
internal fun PlayerSession.sectionKeptInPip(
    inPictureInPicture: Boolean,
    watching: Boolean,
    section: AppSection,
): Boolean = inPictureInPicture && pipReturnsToSection(watching, section)

/**
 * Expanding picture-in-picture back into the app shows the player that was in it full screen, unless it was
 * minimized over a section it returns to.
 */
internal fun PlayerSession.pipExpanded(watching: Boolean, section: AppSection): PlayerSession =
    if (pipReturnsToSection(watching, section)) {
        this
    } else {
        copy(
            liveMinimized = !watching && liveMinimized,
            videoMinimized = video == null && videoMinimized,
        )
    }
