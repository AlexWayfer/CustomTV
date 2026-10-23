package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText

private val CHAT_REPLY_SWIPE_THRESHOLD = 64.dp
private val CHAT_REPLY_SWIPE_ICON_SIZE = 20.dp

/** The circle under the arrow, so it reads over chat rows or the video behind them. */
private val CHAT_REPLY_SWIPE_CIRCLE_SIZE = 32.dp

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

internal enum class ChatSwipeRelease { Reply, Copy, Dismiss, Back }

/** What a swipe toward the end does: nothing, slides the row out to dismiss it, or copies its text. */
internal enum class ChatSwipeTowardEnd { None, Dismiss, Copy }

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
        towardEnd == ChatSwipeTowardEnd.Copy -> ChatSwipeRelease.Copy
        towardEnd == ChatSwipeTowardEnd.Dismiss -> ChatSwipeRelease.Dismiss
        else -> ChatSwipeRelease.Back
    }

/**
 * How far the row slides toward the start. Toward the end, to copy, it slides as it does to reply; to dismiss, it
 * follows the finger.
 */
internal fun chatSwipeOffset(dragPx: Float, thresholdPx: Float, towardEnd: ChatSwipeTowardEnd): Float = when {
    dragPx >= 0f -> chatReplySwipeOffset(dragPx, thresholdPx)
    towardEnd == ChatSwipeTowardEnd.Copy -> -chatReplySwipeOffset(-dragPx, thresholdPx)
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
 * the threshold there slides it out and calls [onDismiss]. [onSwipingChange] reports the drag, so the list
 * can hold its scroll still, and [onSwipeEnd] reports how it ended, before the action it starts.
 * [onOffsetChange] follows the slide, toward the start positive, and reports rest again once the row leaves.
 */
@Composable
internal fun ChatReplySwipe(
    enabled: Boolean,
    onReply: (() -> Unit)?,
    onSwipingChange: (Boolean) -> Unit,
    onDismiss: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onSwipeEnd: (ChatSwipeRelease) -> Unit = {},
    onOffsetChange: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { CHAT_REPLY_SWIPE_THRESHOLD.toPx() }
    val circlePx = with(density) { CHAT_REPLY_SWIPE_CIRCLE_SIZE.toPx() }
    val towardStart = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
    val canReply = onReply != null
    val towardEnd = when {
        onDismiss != null -> ChatSwipeTowardEnd.Dismiss
        onCopy != null -> ChatSwipeTowardEnd.Copy
        else -> ChatSwipeTowardEnd.None
    }
    val swipe = rememberChatSwipe(onReply, onSwipingChange, onDismiss, onCopy, onSwipeEnd, onOffsetChange)
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
        // At rest the icons sit clipped beyond the row's edges; rows not being swiped skip them.
        if (swipe.replyArrowShown) {
            SwipeIcon(R.drawable.ic_reply_arrow, atEnd = true, iconOffsetPx = {
                -towardStart * chatReplyArrowOffset(swipe.offset.value, thresholdPx, circlePx)
            })
        }
        if (towardEnd == ChatSwipeTowardEnd.Copy && swipe.towardEndShown) {
            SwipeIcon(R.drawable.ic_chat_copy, atEnd = false, iconOffsetPx = {
                towardStart * chatReplyArrowOffset(-swipe.offset.value, thresholdPx, circlePx)
            })
        }
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
    }
}

/** The swipe's [icon] in its circle at the row's end, or its start, shifted by [iconOffsetPx] as the row slides. */
@Composable
private fun BoxScope.SwipeIcon(icon: Int, atEnd: Boolean, iconOffsetPx: () -> Float) {
    val edgePadding = (CHAT_REPLY_SWIPE_THRESHOLD - CHAT_REPLY_SWIPE_CIRCLE_SIZE) / 2
    Box(
        modifier = Modifier
            .matchParentSize()
            .clipToBounds()
            .then(if (atEnd) Modifier.padding(end = edgePadding) else Modifier.padding(start = edgePadding)),
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                // A one-line row is lower than the circle: it shrinks to the row height and stays round.
                .sizeIn(maxWidth = CHAT_REPLY_SWIPE_CIRCLE_SIZE, maxHeight = CHAT_REPLY_SWIPE_CIRCLE_SIZE)
                .aspectRatio(1f)
                .graphicsLayer { translationX = iconOffsetPx() }
                .background(TwitchSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = TwitchText,
                modifier = Modifier.size(CHAT_REPLY_SWIPE_ICON_SIZE),
            )
        }
    }
}
