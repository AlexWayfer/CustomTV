package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

internal val CHAT_REPLY_SWIPE_THRESHOLD = 64.dp
internal val CHAT_REPLY_SWIPE_ICON_SIZE = 20.dp

/** The circle under the arrow, so it reads over chat rows or the video behind them. */
internal val CHAT_REPLY_SWIPE_CIRCLE_SIZE = 32.dp

/** Share of the finger travel past the threshold that still moves the row. */
private const val CHAT_REPLY_SWIPE_OVERSHOOT = 0.3f

/** Sideways travel that starts a swipe, in touch slops: a scroll that drifts sideways stays a scroll. */
private const val CHAT_SWIPE_START_SLOPS = 2f

/** A swipe starts only while the finger has moved at least this many times farther sideways than vertically. */
private const val CHAT_SWIPE_HORIZONTAL_RATIO = 2f

internal enum class ChatSwipeStart { Wait, Start, Ignore }

/**
 * Whether a touch that has moved [dxPx] sideways and [dyPx] vertically starts a swipe. Vertical travel
 * past [touchSlopPx] before the swipe starts leaves the gesture to the list's scroll.
 */
internal fun chatSwipeStart(dxPx: Float, dyPx: Float, touchSlopPx: Float): ChatSwipeStart {
    val dx = abs(dxPx)
    val dy = abs(dyPx)
    return when {
        dx >= touchSlopPx * CHAT_SWIPE_START_SLOPS && dx >= dy * CHAT_SWIPE_HORIZONTAL_RATIO -> ChatSwipeStart.Start
        dy >= touchSlopPx -> ChatSwipeStart.Ignore
        else -> ChatSwipeStart.Wait
    }
}

/** How far the row slides toward the start for [dragPx] of finger travel toward the start. */
internal fun chatReplySwipeOffset(dragPx: Float, thresholdPx: Float): Float {
    if (dragPx <= 0f) return 0f
    if (dragPx <= thresholdPx) return dragPx
    return thresholdPx + (dragPx - thresholdPx) * CHAT_REPLY_SWIPE_OVERSHOOT
}

/** Releasing the row after [dragPx] of travel toward the start starts a reply. */
internal fun chatReplySwipeReplies(dragPx: Float, thresholdPx: Float): Boolean =
    thresholdPx > 0f && dragPx >= thresholdPx

/**
 * How far past its resting place the reply arrow sits, toward the end, when the row has slid [offsetPx].
 * It starts just beyond the end edge and reaches its place, centered in the revealed strip, at the threshold.
 */
internal fun chatReplyArrowOffset(offsetPx: Float, thresholdPx: Float, iconPx: Float): Float {
    if (thresholdPx <= 0f) return 0f
    val progress = (offsetPx / thresholdPx).coerceIn(0f, 1f)
    return (1f - progress) * (thresholdPx + iconPx) / 2f
}

internal enum class ChatSwipeRelease { Reply, Copy, Dismiss, Step, Back }

/**
 * What a swipe toward the end does: nothing, slides the row out to dismiss it, copies its text, or picks one of
 * [Steps], the first at the threshold and each next one [Steps.gapPx] farther.
 */
internal sealed interface ChatSwipeTowardEnd {
    data object None : ChatSwipeTowardEnd

    data object Dismiss : ChatSwipeTowardEnd

    data object Copy : ChatSwipeTowardEnd

    data class Steps(val count: Int, val gapPx: Float) : ChatSwipeTowardEnd
}

/**
 * The drag after the finger moves [amountTowardStartPx], as signed travel: toward the start replies,
 * toward the end does [towardEnd]. A direction with no action stays at rest.
 */
internal fun chatSwipeDrag(
    dragPx: Float,
    amountTowardStartPx: Float,
    canReply: Boolean,
    towardEnd: ChatSwipeTowardEnd,
): Float =
    (dragPx + amountTowardStartPx).coerceIn(
        if (towardEnd != ChatSwipeTowardEnd.None) Float.NEGATIVE_INFINITY else 0f,
        if (canReply) Float.POSITIVE_INFINITY else 0f,
    )

/** What releasing the row after [dragPx] of signed travel does. */
internal fun chatSwipeRelease(dragPx: Float, thresholdPx: Float, towardEnd: ChatSwipeTowardEnd): ChatSwipeRelease =
    when {
        chatReplySwipeReplies(dragPx, thresholdPx) -> ChatSwipeRelease.Reply
        !chatReplySwipeReplies(-dragPx, thresholdPx) -> ChatSwipeRelease.Back
        else -> when (towardEnd) {
            ChatSwipeTowardEnd.Copy -> ChatSwipeRelease.Copy
            ChatSwipeTowardEnd.Dismiss -> ChatSwipeRelease.Dismiss
            is ChatSwipeTowardEnd.Steps -> {
                val step = chatSwipeStep(-dragPx, thresholdPx, towardEnd)
                if (step >= 0) ChatSwipeRelease.Step else ChatSwipeRelease.Back
            }
            ChatSwipeTowardEnd.None -> ChatSwipeRelease.Back
        }
    }

/**
 * How far the row slides toward the start. Toward the end, to copy, it slides as it does to reply; with steps, it
 * follows the finger to the last step and slows past it as a reply does past its threshold; to dismiss, it follows
 * the finger.
 */
internal fun chatSwipeOffset(dragPx: Float, thresholdPx: Float, towardEnd: ChatSwipeTowardEnd): Float = when {
    dragPx >= 0f -> chatReplySwipeOffset(dragPx, thresholdPx)
    towardEnd == ChatSwipeTowardEnd.Copy -> -chatReplySwipeOffset(-dragPx, thresholdPx)
    towardEnd is ChatSwipeTowardEnd.Steps ->
        -chatReplySwipeOffset(-dragPx, chatSwipeLastStepPx(thresholdPx, towardEnd))
    else -> dragPx
}

/**
 * Whether a swipe toward the end copies a message with [copyText]: not in full screen, where a sideways swipe moves or
 * hides the chat itself.
 */
internal fun chatSwipeCopies(copyText: String?, fullscreen: Boolean): Boolean = copyText != null && !fullscreen

/** A row sliding away to dismiss fades out as it nears the end edge. */
internal fun chatSwipeDismissAlpha(offsetPx: Float, widthPx: Int): Float {
    if (offsetPx >= 0f || widthPx <= 0) return 1f
    return (1f + offsetPx / widthPx).coerceIn(0f, 1f)
}

/**
 * Slides [content] toward the start under the finger while a reply arrow slides in from the end.
 * Releasing past the threshold calls [onReply]; the row springs back either way. With [onCopy], the row
 * slides toward the end the same way, under a copy icon from the start, and releasing past the threshold
 * calls [onCopy]. With [onDismiss] instead, the row follows the finger toward the end, and releasing past
 * the threshold there slides it out and calls [onDismiss]. With [steps] instead of a copy, the row follows the
 * finger toward the end through each step in turn, under the step's icon and name, and releasing on one picks it.
 * [onSwipingChange] reports the drag, so the list can hold its scroll still, and [onSwipeEnd] reports how it
 * ended, before the action it starts. [onOffsetChange] follows the slide, toward the start positive, and reports
 * rest again once the row leaves.
 */
@Composable
internal fun ChatReplySwipe(
    enabled: Boolean,
    onReply: (() -> Unit)?,
    onSwipingChange: (Boolean) -> Unit,
    onDismiss: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    steps: ChatSwipeSteps? = null,
    onSwipeEnd: (ChatSwipeRelease) -> Unit = {},
    onOffsetChange: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { CHAT_REPLY_SWIPE_THRESHOLD.toPx() }
    val circlePx = with(density) { CHAT_REPLY_SWIPE_CIRCLE_SIZE.toPx() }
    val stepGapPx = with(density) { CHAT_SWIPE_STEP_GAP.toPx() }
    val towardStart = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
    val canReply = onReply != null
    val towardEnd = chatSwipeTowardEnd(onDismiss != null, steps?.count ?: 0, onCopy != null, stepGapPx)
    val swipe = rememberChatSwipe(
        onReply = onReply,
        onSwipingChange = onSwipingChange,
        onDismiss = onDismiss,
        onCopy = onCopy,
        onStep = steps?.onStep,
        onSwipeEnd = onSwipeEnd,
        onOffsetChange = onOffsetChange,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.pointerInput(thresholdPx, towardStart, canReply, towardEnd) {
                        swipe.handle(this, thresholdPx, towardStart, canReply, towardEnd)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        ReplyAndCopyIcons(swipe, copies = towardEnd == ChatSwipeTowardEnd.Copy, thresholdPx, circlePx, towardStart)
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = swipe.offset.value * towardStart
                if (towardEnd == ChatSwipeTowardEnd.Dismiss) {
                    alpha = chatSwipeDismissAlpha(swipe.offset.value, size.width.roundToInt())
                }
            },
        ) {
            content()
        }
        // Over the row, so the step's name reads even where the slid row has not uncovered it yet.
        if (towardEnd is ChatSwipeTowardEnd.Steps && steps != null && swipe.towardEndShown) {
            SwipeStepIndicator(steps, towardEnd, thresholdPx, offsetPx = { swipe.offset.value }, iconOffsetPx = {
                towardStart * chatReplyArrowOffset(-swipe.offset.value, thresholdPx, circlePx)
            })
        }
    }
}
