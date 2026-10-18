package name.alexwayfer.customtv.player

import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import kotlin.math.hypot

/**
 * Zooms the player's picture: two fingers pinch [view] around them, and one finger moves the zoomed picture. While
 * the picture is zoomed, every touch on it is the zoom's. [onSettled] hears where the zoom rests: once the fingers
 * lift, and on a reset.
 */
internal class PlayerZoomGesture(
    private val view: View,
    private val onSettled: (zoomed: Boolean) -> Unit = {},
    private val onChange: (PlayerZoomTransform) -> Unit,
) {
    var transform = PlayerZoomTransform()
        private set

    /** The current touch is the zoom's: a pinch, or any touch on a zoomed picture. */
    var active = false
        private set

    private var start = PlayerZoomTransform()
    private var firstId = MotionEvent.INVALID_POINTER_ID
    private var secondId = MotionEvent.INVALID_POINTER_ID
    private var startFocus = ZoomPoint(0f, 0f)
    private var startSpan = 1f

    init {
        // A new size, such as after a rotation, keeps the zoom and only moves the picture back over the player.
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> show(transform.within(frame())) }
    }

    /**
     * Whether [event] is the zoom's. [canZoom]: the player can zoom now, so a touch on a zoomed picture moves it and a
     * second finger starts a pinch.
     */
    fun onTouch(event: MotionEvent, canZoom: Boolean): Boolean {
        val wasActive = active
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (canZoom) {
                // A touch during the way back to the whole picture catches it where it is.
                view.animate().cancel()
                val current = PlayerZoomTransform(view.scaleX, view.translationX, view.translationY)
                if (current != transform) {
                    transform = current
                    onChange(current)
                }
                active = current.zoomed
                if (active) follow(event, event.getPointerId(0))
            }
            MotionEvent.ACTION_POINTER_DOWN -> if ((active || canZoom) && secondId == MotionEvent.INVALID_POINTER_ID) {
                active = true
                val added = event.getPointerId(event.actionIndex)
                val first = firstId.takeIf { it != MotionEvent.INVALID_POINTER_ID }
                    ?: event.getPointerId(if (event.actionIndex == 0) 1 else 0)
                follow(event, first, added)
            }
            MotionEvent.ACTION_MOVE -> if (active) move(event)
            MotionEvent.ACTION_POINTER_UP -> if (active) lift(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (active) {
                active = false
                firstId = MotionEvent.INVALID_POINTER_ID
                secondId = MotionEvent.INVALID_POINTER_ID
                animateTo(settledPlayerZoom(transform))
                onSettled(transform.zoomed)
            }
        }
        return active || wasActive
    }

    /** Brings back the whole picture. */
    fun reset() {
        animateTo(PlayerZoomTransform())
        onSettled(false)
    }

    /** Starts following [first], and [second] for a pinch, from where they are now. */
    private fun follow(event: MotionEvent, first: Int, second: Int = MotionEvent.INVALID_POINTER_ID) {
        firstId = first
        secondId = second
        start = transform
        val a = point(event, first) ?: return
        val b = point(event, second)
        if (b == null) {
            startFocus = a
        } else {
            startFocus = middle(a, b)
            startSpan = distance(a, b).coerceAtLeast(1f)
        }
    }

    private fun move(event: MotionEvent) {
        val a = point(event, firstId) ?: return
        val b = point(event, secondId)
        show(
            if (b == null) {
                pannedPlayerZoom(start, a.x - startFocus.x, a.y - startFocus.y, frame())
            } else {
                pinchedPlayerZoom(start, PlayerPinch(startSpan, distance(a, b), startFocus, middle(a, b)), frame())
            },
        )
    }

    /** A finger of the pinch lifts: the one left moves the picture on from where it is. */
    private fun lift(event: MotionEvent) {
        val lifted = event.getPointerId(event.actionIndex)
        if (lifted != firstId && lifted != secondId) return
        val other = if (lifted == firstId) secondId else firstId
        val left = other.takeIf { it != MotionEvent.INVALID_POINTER_ID }
            ?: (0 until event.pointerCount).map(event::getPointerId).first { it != lifted }
        follow(event, left)
    }

    /** Where the finger [id] touches the screen, from the player's center; [event] comes in the zoomed view's terms. */
    private fun point(event: MotionEvent, id: Int): ZoomPoint? {
        val index = if (id == MotionEvent.INVALID_POINTER_ID) -1 else event.findPointerIndex(id)
        if (index < 0) return null
        return playerZoomPoint(event.getX(index), event.getY(index), transform, frame())
    }

    private fun frame() = PlayerZoomFrame(view.width.toFloat(), view.height.toFloat())

    private fun show(next: PlayerZoomTransform) {
        if (next == transform) return
        transform = next
        view.scaleX = next.scale
        view.scaleY = next.scale
        view.translationX = next.x
        view.translationY = next.y
        onChange(next)
    }

    private fun animateTo(target: PlayerZoomTransform) {
        if (target == transform) return
        transform = target
        onChange(target)
        view.animate()
            .scaleX(target.scale)
            .scaleY(target.scale)
            .translationX(target.x)
            .translationY(target.y)
            .setDuration(view.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .setInterpolator(StandardEasing)
            .start()
    }

    private fun middle(a: ZoomPoint, b: ZoomPoint) = ZoomPoint((a.x + b.x) / 2, (a.y + b.y) / 2)

    private fun distance(a: ZoomPoint, b: ZoomPoint) = hypot(a.x - b.x, a.y - b.y)
}

/** Material's standard easing. */
private val StandardEasing = PathInterpolator(0.2f, 0f, 0f, 1f)
