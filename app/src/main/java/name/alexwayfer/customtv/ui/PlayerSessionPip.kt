package name.alexwayfer.customtv.ui

/** Closing picture-in-picture stops the stream; returning to the app shows it again full screen. */
internal fun PlayerSession.livePipClosed(): PlayerSession =
    copy(liveClosedWithPip = true, liveMinimized = false)

internal fun PlayerSession.livePipResumed(): PlayerSession = copy(liveClosedWithPip = false)

/** Expanding picture-in-picture back into the app shows the player that was in it full screen. */
internal fun PlayerSession.pipExpanded(watching: Boolean): PlayerSession = copy(
    liveMinimized = !watching && liveMinimized,
    videoMinimized = video == null && videoMinimized,
)
