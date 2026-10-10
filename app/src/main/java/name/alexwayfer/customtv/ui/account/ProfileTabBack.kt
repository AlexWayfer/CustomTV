package name.alexwayfer.customtv.ui.account

internal const val PROFILE_TAB_HOME = 0
internal const val PROFILE_TAB_ABOUT = 1

/** The tab a profile opens on. Whatever tab comes first; today that is Home. */
internal const val PROFILE_TAB_FIRST = PROFILE_TAB_HOME

/** Back on another profile tab returns to the first tab. On the first tab it leaves the profile (null). */
internal fun profileTabAfterBack(selectedTab: Int): Int? =
    if (selectedTab == PROFILE_TAB_FIRST) null else PROFILE_TAB_FIRST
