package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.ui.AppSection

fun followedListKeepsTop(firstVisibleIndex: Int, scrollOffset: Int): Boolean {
    return firstVisibleIndex == 0 && scrollOffset == 0
}

internal fun homeTapScrollsListToTop(section: AppSection): Boolean = section == AppSection.Home
