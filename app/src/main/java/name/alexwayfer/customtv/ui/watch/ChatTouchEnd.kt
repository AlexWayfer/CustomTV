package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** How a touch on the chat ended, as far as the message field's focus cares. */
internal enum class ChatTouchEnd { Released, Moved, Held }

/**
 * Only a tap on the chat takes the focus from the message field, so the keyboard goes. A scroll or a reply swipe
 * keeps it, so the rows do not jump under the finger as the chat grows; a finger held still for a long press keeps
 * it too: the message menu opens over the keyboard, and the message being typed stays at hand.
 */
internal fun chatTouchClearsFocus(end: ChatTouchEnd): Boolean = end == ChatTouchEnd.Released

/**
 * How a touch still under the long press timeout has ended so far: lifted, moved past the touch slop, or not yet
 * (null). A touch that runs out the timeout without either is [ChatTouchEnd.Held].
 */
internal fun chatTouchEndBeforeLongPress(released: Boolean, distancePx: Float, touchSlopPx: Float): ChatTouchEnd? =
    when {
        released -> ChatTouchEnd.Released
        distancePx > touchSlopPx -> ChatTouchEnd.Moved
        else -> null
    }

/** Clears the field's focus as [chatTouchClearsFocus] decides, once the touch is told from a long press. */
internal fun Modifier.clearFocusOnTouch(focusManager: FocusManager): Modifier = pointerInput(focusManager) {
    awaitEachGesture {
        // Read after every other handler, so a row or the list taking the touch does not hide it from here.
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        val end = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var ended: ChatTouchEnd? = null
            while (ended == null) {
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                ended = chatTouchEndBeforeLongPress(
                    released = change == null || !change.pressed,
                    distancePx = change?.let { (it.position - down.position).getDistance() } ?: 0f,
                    touchSlopPx = viewConfiguration.touchSlop,
                )
            }
            ended
        } ?: ChatTouchEnd.Held
        if (chatTouchClearsFocus(end)) focusManager.clearFocus(force = true)
    }
}
