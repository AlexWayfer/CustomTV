package name.alexwayfer.customtv.ui.home

/** What the full-screen search looks for. */
internal enum class SearchMode { Channels, Categories }

/**
 * The mode the search shows: the one [picked] since it opened, otherwise the one it [opensIn]. Without
 * [categoriesOffered] there is only the channel search.
 */
internal fun shownSearchMode(picked: SearchMode?, opensIn: SearchMode, categoriesOffered: Boolean): SearchMode =
    if (categoriesOffered) picked ?: opensIn else SearchMode.Channels
