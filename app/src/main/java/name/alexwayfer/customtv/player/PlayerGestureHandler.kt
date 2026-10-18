package name.alexwayfer.customtv.player

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

/** [VelocityTracker] units for a velocity in pixels per second. */
private const val PIXELS_PER_SECOND = 1000

/** What the gestures read from the player at each touch. */
internal data class PlayerTouchState(
    val isReleased: () -> Boolean,
    /** The app taps the page itself to match the controls; that tap goes straight to the page. */
    val isAppTap: () -> Boolean,
    val canMinimize: () -> Boolean,
    val canExpand: () -> Boolean,
    val canDismissHorizontally: () -> Boolean,
    /** Two fingers can zoom the picture, and one can move it once zoomed. */
    val canZoom: () -> Boolean,
    val controlsVisible: () -> Boolean,
    /** Whether the page saw the current touch land on the player controls; it reports that shortly after the down. */
    val touchOnControls: () -> Boolean,
    val touchOnScrollableContent: () -> Boolean,
    /** Whether the page saw the current touch land on a disclosure, such as the audience plaque. */
    val touchOnDisclosure: () -> Boolean,
    /** How tall the system's status bar is, shown or not: a swipe down from that band is the system's. */
    val statusBarPx: () -> Int,
    val tapExpands: () -> Boolean,
    /** The tap only shows or hides the chrome; the page, which would act on it, does not get it. */
    val tapStaysOffPage: () -> Boolean,
)

/** What the gestures tell the player: a tap, and the drags with their offset and, at the end, their velocity. */
internal data class PlayerTouchCallbacks(
    /** `reachedPage`: the page got the tap too and acts on it on its own, such as toggling its controls. */
    val onTap: (reachedPage: Boolean, x: Float) -> Unit,
    val onVerticalDrag: (Float) -> Unit,
    val onVerticalDragEnd: (Float, Float) -> Unit,
    val onHorizontalDrag: (Float) -> Unit,
    val onHorizontalDragEnd: (Float, Float) -> Unit,
)

internal class PlayerGestureHandler(
    private val view: View,
    private val touchSlop: Int,
    private val state: PlayerTouchState,
    private val callbacks: PlayerTouchCallbacks,
    private val zoom: PlayerZoomGesture,
) {
    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var downRawY = 0f

    /** The finger the touch follows, the first one down; another finger neither moves nor ends it. */
    private var touchPointerId = MotionEvent.INVALID_POINTER_ID
    private var controlsShownAtDown = false
    private var downInStatusBarZone = false
    private var dragAxis = DragAxis.None
    private var velocityTracker: VelocityTracker? = null

    @SuppressLint("ClickableViewAccessibility")
    fun install() {
        view.setOnTouchListener { _, event -> handle(event) }
    }

    fun release() {
        view.setOnTouchListener(null)
        velocityTracker?.recycle()
        velocityTracker = null
        dragAxis = DragAxis.None
        touchPointerId = MotionEvent.INVALID_POINTER_ID
    }

    private fun handle(event: MotionEvent): Boolean {
        if (state.isReleased() || state.isAppTap()) return false
        if (takenByZoom(event)) return true
        val action = event.actionMasked
        return when {
            action == MotionEvent.ACTION_DOWN -> {
                onDown(event)
                false
            }
            // The followed finger lifted already; the rest of the touch is another finger's.
            touchPointerId == MotionEvent.INVALID_POINTER_ID -> false
            playerTouchEnds(action, event.getPointerId(event.actionIndex), touchPointerId) -> onUpOrCancel(event)
            action == MotionEvent.ACTION_MOVE -> onMove(event)
            else -> false
        }
    }

    /**
     * A pinch, or any touch on a zoomed picture, is the zoom's alone: neither the page nor a drag of the player gets
     * it. A pinch can start on a touch still with the page, such as on its controls, but not on a drag of the player.
     */
    private fun takenByZoom(event: MotionEvent): Boolean {
        val wasZooming = zoom.active
        val canZoom = state.canZoom() && (dragAxis == DragAxis.None || dragAxis == DragAxis.Page)
        if (!zoom.onTouch(event, canZoom)) return false
        val parent = view.parent as? ViewGroup
        if (!wasZooming) {
            parent?.requestDisallowInterceptTouchEvent(true)
            // A pinch takes over a touch the page already has; a touch on a zoomed picture never reaches it.
            if (event.actionMasked != MotionEvent.ACTION_DOWN) view.cancelPageTouch(event)
            dragAxis = DragAxis.None
            touchPointerId = MotionEvent.INVALID_POINTER_ID
            velocityTracker?.recycle()
            velocityTracker = null
        }
        if (!zoom.active) parent?.requestDisallowInterceptTouchEvent(false)
        return true
    }

    private fun onDown(event: MotionEvent) {
        touchPointerId = event.getPointerId(0)
        downX = event.x
        downY = event.y
        downRawX = event.rawX
        downRawY = event.rawY
        controlsShownAtDown = state.controlsVisible()
        downInStatusBarZone = touchStartsInStatusBarZone(event.rawY, state.statusBarPx())
        dragAxis = DragAxis.None
        velocityTracker?.recycle()
        velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
    }

    private fun onMove(event: MotionEvent): Boolean {
        velocityTracker?.addMovement(event)
        val dy = touchRawY(event) - downRawY
        val dx = touchRawX(event) - downRawX
        if (dragAxis == DragAxis.None && touchStaysWithControls(controlsShownAtDown, state.touchOnControls())) {
            dragAxis = DragAxis.Page
        }
        if (dragAxis == DragAxis.None) {
            val absDx = abs(dx)
            val absDy = abs(dy)
            val startVertical = shouldStartPlayerVerticalDrag(
                deltaY = dy,
                deltaX = dx,
                rules = PlayerSwipeRules(touchSlop, state.canMinimize(), state.canExpand()),
                touchOnScrollableContent = state.touchOnScrollableContent(),
                startsInStatusBarZone = downInStatusBarZone,
            )
            val startHorizontal = state.canDismissHorizontally() &&
                absDx > touchSlop && absDx > absDy
            if (startVertical || startHorizontal) {
                dragAxis = if (startVertical) DragAxis.Vertical else DragAxis.Horizontal
                (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                view.cancelPageTouch(event)
            }
        }
        return when (dragAxis) {
            DragAxis.Vertical -> {
                callbacks.onVerticalDrag(dy)
                true
            }
            DragAxis.Horizontal -> {
                callbacks.onHorizontalDrag(dx)
                true
            }
            DragAxis.None, DragAxis.Page -> false
        }
    }

    private fun onUpOrCancel(event: MotionEvent): Boolean {
        (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
        velocityTracker?.addMovement(event)
        val axis = dragAxis
        val pointerId = touchPointerId
        dragAxis = DragAxis.None
        touchPointerId = MotionEvent.INVALID_POINTER_ID
        if (axis == DragAxis.Vertical || axis == DragAxis.Horizontal) {
            val tracker = velocityTracker
            tracker?.computeCurrentVelocity(PIXELS_PER_SECOND)
            if (axis == DragAxis.Vertical) {
                callbacks.onVerticalDragEnd(
                    touchRawY(event, pointerId) - downRawY,
                    tracker?.getYVelocity(pointerId) ?: 0f,
                )
            } else {
                callbacks.onHorizontalDragEnd(
                    touchRawX(event, pointerId) - downRawX,
                    tracker?.getXVelocity(pointerId) ?: 0f,
                )
            }
            tracker?.recycle()
            velocityTracker = null
            return true
        }

        velocityTracker?.recycle()
        velocityTracker = null
        return event.actionMasked == MotionEvent.ACTION_UP && onLiftUp(event)
    }

    /** A finger lifted without a drag, a tap when within the touch slop; true when the page must not see it. */
    private fun onLiftUp(event: MotionEvent): Boolean {
        val dx = event.x - downX
        val dy = event.y - downY
        val end = playerTouchEnd(
            withinTapSlop = dx * dx + dy * dy < touchSlop * touchSlop,
            offPage = state.tapExpands() || state.tapStaysOffPage(),
            onDisclosure = state.touchOnDisclosure(),
        )
        if (end != PlayerTouchEnd.PageTouch && end != PlayerTouchEnd.Dropped) view.performClick()
        return when (end) {
            PlayerTouchEnd.PageTouch, PlayerTouchEnd.DisclosureTap -> false
            PlayerTouchEnd.Dropped -> {
                view.cancelPageTouch(event)
                true
            }
            PlayerTouchEnd.PageTap -> {
                callbacks.onTap(true, event.x)
                false
            }
            PlayerTouchEnd.AppTap -> {
                view.cancelPageTouch(event)
                callbacks.onTap(false, event.x)
                true
            }
        }
    }

    /** Where the followed finger is on the screen; a finger put down beside the player may come first in [event]. */
    private fun touchRawX(event: MotionEvent, pointerId: Int = touchPointerId): Float =
        event.getX(touchIndex(event, pointerId)) + event.rawX - event.x

    private fun touchRawY(event: MotionEvent, pointerId: Int = touchPointerId): Float =
        event.getY(touchIndex(event, pointerId)) + event.rawY - event.y

    private fun touchIndex(event: MotionEvent, pointerId: Int): Int = event.findPointerIndex(pointerId).coerceAtLeast(0)

    /** [Page]: the touch started on the shown controls, such as on the seek bar; the player stays put until it ends. */
    private enum class DragAxis { None, Vertical, Horizontal, Page }
}

/** Ends the touch for the page without a click, so the player does not act on it. */
private fun View.cancelPageTouch(event: MotionEvent) {
    val cancel = MotionEvent.obtain(event)
    cancel.action = MotionEvent.ACTION_CANCEL
    onTouchEvent(cancel)
    cancel.recycle()
}
