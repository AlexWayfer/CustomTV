package name.alexwayfer.customtv.player

import android.view.MotionEvent

/**
 * Whether [actionMasked] ends the touch the player follows, the one whose finger [touchPointerId] went down first: an
 * up, a cancel, or that finger lifting while another, such as one put down beside the player, stays. No later event
 * of the touch reaches the player then, so waiting for an up would leave a drag unfinished.
 */
internal fun playerTouchEnds(actionMasked: Int, liftedPointerId: Int, touchPointerId: Int): Boolean =
    when (actionMasked) {
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
        MotionEvent.ACTION_POINTER_UP -> liftedPointerId == touchPointerId
        else -> false
    }

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
