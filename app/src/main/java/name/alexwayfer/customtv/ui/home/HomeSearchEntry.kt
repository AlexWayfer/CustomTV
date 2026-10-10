package name.alexwayfer.customtv.ui.home

/** How Home offers the channel search. */
internal enum class HomeSearchEntry {
    /** Nothing yet: the recents are still loading, and they decide between the other two. */
    None,

    /** A search button beside the title, so the list below gets the room. */
    Button,

    /** The field in the middle of the empty screen, where searching is the only thing to do. */
    Field,
}

/**
 * The search stays a button while there is a list to show: the followed channels when logged in, or the
 * recents. Only an empty screen, logged out with no recents, shows the field itself.
 */
internal fun homeSearchEntry(signedIn: Boolean, recentsRestored: Boolean, hasRecents: Boolean): HomeSearchEntry =
    when {
        signedIn || hasRecents -> HomeSearchEntry.Button
        !recentsRestored -> HomeSearchEntry.None
        else -> HomeSearchEntry.Field
    }
