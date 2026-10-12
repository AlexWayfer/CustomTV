package name.alexwayfer.customtv.ui

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
