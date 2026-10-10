package name.alexwayfer.customtv.player

internal fun shouldStartPlayerVerticalDrag(
    deltaY: Float,
    deltaX: Float,
    touchSlop: Int,
    canMinimize: Boolean,
    canExpand: Boolean,
    touchOnScrollableContent: Boolean,
    /** The touch began where the system pulls its status bar down; that gesture is the system's. */
    startsInStatusBarZone: Boolean = false,
): Boolean = !touchOnScrollableContent &&
    !startsInStatusBarZone &&
    kotlin.math.abs(deltaY) > touchSlop &&
    kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) &&
    ((deltaY > 0 && canMinimize) || (deltaY < 0 && canExpand))

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
