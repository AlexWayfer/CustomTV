package name.alexwayfer.customtv.ui.home

fun followedListKeepsTop(firstVisibleIndex: Int, scrollOffset: Int): Boolean {
    return firstVisibleIndex == 0 && scrollOffset == 0
}
