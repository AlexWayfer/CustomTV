package name.alexwayfer.customtv.player

internal fun shouldStartPlayerVerticalDrag(
    deltaY: Float,
    deltaX: Float,
    rules: PlayerSwipeRules,
    touchOnScrollableContent: Boolean,
    /** The touch began where the system pulls its status bar down; that gesture is the system's. */
    startsInStatusBarZone: Boolean = false,
): Boolean = !touchOnScrollableContent &&
    !startsInStatusBarZone &&
    kotlin.math.abs(deltaY) > rules.touchSlop &&
    kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) &&
    ((deltaY > 0 && rules.canMinimize) || (deltaY < 0 && rules.canExpand))

/**
 * Whether a touch at [downRawY] on the screen began in the status bar's band, [statusBarPx] tall, where a swipe down
 * brings the system's status bar even while it is hidden in full screen.
 */
internal fun touchStartsInStatusBarZone(downRawY: Float, statusBarPx: Int): Boolean = downRawY < statusBarPx

internal fun minimizeDragHidesKeyboard(fromMiniPlayer: Boolean, offsetY: Float): Boolean =
    !fromMiniPlayer && offsetY > 0f

/** Keep the app navigation bar while the keyboard closes only when the drag closes a keyboard that is up. */
internal fun minimizeDragKeepsNavigationBar(fromMiniPlayer: Boolean, offsetY: Float, imeBottomPx: Int): Boolean =
    minimizeDragHidesKeyboard(fromMiniPlayer, offsetY) && imeBottomPx > 0

/** How far a swipe goes before it counts, and which ways the player can move: down to minimize, up to expand. */
internal data class PlayerSwipeRules(val touchSlop: Int, val canMinimize: Boolean, val canExpand: Boolean)
