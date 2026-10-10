package name.alexwayfer.customtv.ui.home

internal const val RECENT_AUTO_REFRESH_INTERVAL_MS = 60_000L

internal fun shouldAutomaticallyRefreshRecents(lastRequestAtMillis: Long?, nowMillis: Long): Boolean =
    lastRequestAtMillis == null ||
        nowMillis < lastRequestAtMillis ||
        nowMillis - lastRequestAtMillis >= RECENT_AUTO_REFRESH_INTERVAL_MS

internal fun showAutomaticRecentRefreshProgress(
    firstHomeRefresh: Boolean,
    returnedToForeground: Boolean,
): Boolean = firstHomeRefresh || returnedToForeground

/** Recents refresh in the background only while Home lists them: signed out. Logging out lists them again. */
internal fun refreshRecentsInBackground(signedIn: Boolean, hasRecents: Boolean): Boolean = !signedIn && hasRecents

/**
 * Signed in, Home lists no recents, so they refresh when the search suggestions open: those show the recents as
 * history, live when their profile is. Signed out, the background refresh already keeps them current.
 */
internal fun refreshRecentsOnSearchOpen(signedIn: Boolean, expanded: Boolean, hasRecents: Boolean): Boolean =
    signedIn && expanded && hasRecents
