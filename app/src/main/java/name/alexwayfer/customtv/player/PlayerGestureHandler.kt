package name.alexwayfer.customtv.player

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

internal class PlayerGestureHandler(
    private val view: View,
    private val touchSlop: Int,
    private val isReleased: () -> Boolean,
    /** The app taps the page itself to match the controls; that tap goes straight to the page. */
    private val isAppTap: () -> Boolean,
    private val canMinimize: () -> Boolean,
    private val canExpand: () -> Boolean,
    private val canDismissHorizontally: () -> Boolean,
    private val controlsVisible: () -> Boolean,
    /** Whether the page saw the current touch land on the player controls; it reports that shortly after the down. */
    private val touchOnControls: () -> Boolean,
    private val touchOnScrollableContent: () -> Boolean,
    /** How tall the system's status bar is, shown or not: a swipe down from that band is the system's. */
    private val statusBarPx: () -> Int,
    private val tapExpands: () -> Boolean,
    /** The tap only shows or hides the chrome; the page, which would act on it, does not get it. */
    private val tapStaysOffPage: () -> Boolean,
    /** [reachedPage]: the page got the tap too and acts on it on its own, such as toggling its controls. */
    private val onTap: (reachedPage: Boolean, x: Float) -> Unit,
    private val onVerticalDrag: (Float) -> Unit,
    private val onVerticalDragEnd: (Float, Float) -> Unit,
    private val onHorizontalDrag: (Float) -> Unit,
    private val onHorizontalDragEnd: (Float, Float) -> Unit,
) {
    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
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
    }

    private fun handle(event: MotionEvent): Boolean {
        return !isReleased() && !isAppTap() && when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDown(event)
                false
            }
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUpOrCancel(event)
            else -> false
        }
    }

    private fun onDown(event: MotionEvent) {
        downX = event.x
        downY = event.y
        downRawX = event.rawX
        downRawY = event.rawY
        controlsShownAtDown = controlsVisible()
        downInStatusBarZone = touchStartsInStatusBarZone(event.rawY, statusBarPx())
        dragAxis = DragAxis.None
        velocityTracker?.recycle()
        velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
    }

    private fun onMove(event: MotionEvent): Boolean {
        velocityTracker?.addMovement(event)
        val dy = event.rawY - downRawY
        val dx = event.rawX - downRawX
        if (dragAxis == DragAxis.None && touchStaysWithControls(controlsShownAtDown, touchOnControls())) {
            dragAxis = DragAxis.Page
        }
        if (dragAxis == DragAxis.None) {
            val absDx = abs(dx)
            val absDy = abs(dy)
            val startVertical = shouldStartPlayerVerticalDrag(
                deltaY = dy,
                deltaX = dx,
                touchSlop = touchSlop,
                canMinimize = canMinimize(),
                canExpand = canExpand(),
                touchOnScrollableContent = touchOnScrollableContent(),
                startsInStatusBarZone = downInStatusBarZone,
            )
            val startHorizontal = canDismissHorizontally() &&
                absDx > touchSlop && absDx > absDy
            if (startVertical || startHorizontal) {
                dragAxis = if (startVertical) DragAxis.Vertical else DragAxis.Horizontal
                (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                cancelPageTouch(event)
            }
        }
        return when (dragAxis) {
            DragAxis.Vertical -> {
                onVerticalDrag(dy)
                true
            }
            DragAxis.Horizontal -> {
                onHorizontalDrag(dx)
                true
            }
            DragAxis.None, DragAxis.Page -> false
        }
    }

    private fun onUpOrCancel(event: MotionEvent): Boolean {
        (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
        velocityTracker?.addMovement(event)
        val axis = dragAxis
        dragAxis = DragAxis.None
        if (axis == DragAxis.Vertical || axis == DragAxis.Horizontal) {
            val tracker = velocityTracker
            tracker?.computeCurrentVelocity(1000)
            if (axis == DragAxis.Vertical) {
                onVerticalDragEnd(event.rawY - downRawY, tracker?.yVelocity ?: 0f)
            } else {
                onHorizontalDragEnd(event.rawX - downRawX, tracker?.xVelocity ?: 0f)
            }
            tracker?.recycle()
            velocityTracker = null
            return true
        }

        velocityTracker?.recycle()
        velocityTracker = null
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val dx = event.x - downX
            val dy = event.y - downY
            if (dx * dx + dy * dy < touchSlop * touchSlop) {
                view.performClick()
                if (tapExpands() || tapStaysOffPage()) {
                    cancelPageTouch(event)
                    onTap(false, event.x)
                    return true
                }
                onTap(true, event.x)
            }
        }
        return false
    }

    /** Ends the touch for the page without a click, so the player does not act on it. */
    private fun cancelPageTouch(event: MotionEvent) {
        val cancel = MotionEvent.obtain(event)
        cancel.action = MotionEvent.ACTION_CANCEL
        view.onTouchEvent(cancel)
        cancel.recycle()
    }

    /** [Page]: the touch started on the shown controls, such as on the seek bar; the player stays put until it ends. */
    private enum class DragAxis { None, Vertical, Horizontal, Page }
}
