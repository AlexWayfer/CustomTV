package name.alexwayfer.customtv.ui

/**
 * [keyboardInHomeSearch]: the keyboard belongs to the full-screen home search's window. The app window
 * then gets the keyboard height at once, without its animation, so it ignores it: the keyboard
 * covers the bottom of the screen instead.
 */
internal fun showAppNavigationBar(
    inPictureInPicture: Boolean,
    imeBottomPx: Int,
    imeTargetBottomPx: Int,
    keepVisibleWhileImeCloses: Boolean,
    keyboardInHomeSearch: Boolean,
): Boolean = !inPictureInPicture && (
    imeBottomPx <= 0 || imeTargetBottomPx <= 0 || keepVisibleWhileImeCloses || keyboardInHomeSearch
    )

/** Where Material's compact window width ends. */
private const val COMPACT_WIDTH_DP = 600f

/**
 * The navigation sits at the bottom of a compact window, a phone held upright of any size, and on a rail along
 * the start edge from Material's medium width on: a phone on its side, an unfolded foldable, a tablet. It goes by
 * the app window's width, so a split screen counts only its own part.
 */
internal fun appNavigationUsesRail(windowWidthDp: Float): Boolean = windowWidthDp >= COMPACT_WIDTH_DP

internal fun homeUsesImePadding(
    keepNavigationBarWhileImeCloses: Boolean,
    keyboardInHomeSearch: Boolean,
): Boolean = !keepNavigationBarWhileImeCloses && !keyboardInHomeSearch
