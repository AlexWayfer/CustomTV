package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints
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

/**
 * Follows the finger [downId] until it starts a swipe, and returns the change that started it. Null when the finger
 * lifts, another handler takes the touch, or it moves vertically first.
 */
private suspend fun AwaitPointerEventScope.awaitSwipeStart(downId: PointerId, touchSlopPx: Float): PointerInputChange? {
    var travel = Offset.Zero
    var start = ChatSwipeStart.Wait
    var change: PointerInputChange? = null
    while (start == ChatSwipeStart.Wait) {
        val next = awaitPointerEvent().changes.firstOrNull { it.id == downId }
        if (next == null || !next.pressed || next.isConsumed) return null
        travel += next.positionChange()
        start = chatSwipeStart(travel.x, travel.y, touchSlopPx)
        change = next
    }
    return change.takeIf { start == ChatSwipeStart.Start }
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

internal enum class ChatSwipeRelease { Reply, Dismiss, Back }

/**
 * The drag after the finger moves [amountTowardStartPx], as signed travel: toward the start replies,
 * toward the end dismisses. A direction with no action stays at rest.
 */
internal fun chatSwipeDrag(dragPx: Float, amountTowardStartPx: Float, canReply: Boolean, canDismiss: Boolean): Float =
    (dragPx + amountTowardStartPx).coerceIn(
        if (canDismiss) Float.NEGATIVE_INFINITY else 0f,
        if (canReply) Float.POSITIVE_INFINITY else 0f,
    )

/** What releasing the row after [dragPx] of signed travel does. */
internal fun chatSwipeRelease(dragPx: Float, thresholdPx: Float): ChatSwipeRelease = when {
    chatReplySwipeReplies(dragPx, thresholdPx) -> ChatSwipeRelease.Reply
    chatReplySwipeReplies(-dragPx, thresholdPx) -> ChatSwipeRelease.Dismiss
    else -> ChatSwipeRelease.Back
}

/** How far the row slides toward the start; toward the end, to dismiss, it follows the finger. */
internal fun chatSwipeOffset(dragPx: Float, thresholdPx: Float): Float =
    if (dragPx >= 0f) chatReplySwipeOffset(dragPx, thresholdPx) else dragPx

/** A row sliding away to dismiss fades out as it nears the end edge. */
internal fun chatSwipeDismissAlpha(offsetPx: Float, widthPx: Int): Float {
    if (offsetPx >= 0f || widthPx <= 0) return 1f
    return (1f + offsetPx / widthPx).coerceIn(0f, 1f)
}

/**
 * The row's slide under the finger and what releasing it does. The callbacks read the latest the row was composed
 * with; [onArmed] ticks when the travel crosses the threshold, and [onSwipeEnd] reports how the swipe ended.
 */
private class ChatSwipe(
    private val scope: CoroutineScope,
    private val onSwipingChange: (Boolean) -> Unit,
    private val onSwipeEnd: (ChatSwipeRelease) -> Unit,
    private val onArmed: () -> Unit,
    private val onReply: () -> Unit,
    private val onDismiss: () -> Unit,
) {
    val offset = Animatable(0f)
    val arrowShown by derivedStateOf { offset.value > 0f }
    private var swiping = false

    private fun startSwiping() {
        if (swiping) return
        swiping = true
        onSwipingChange(true)
    }

    /** Ends the swipe once, with what its release does; a cancelled swipe ends as [ChatSwipeRelease.Back]. */
    fun endSwiping(release: ChatSwipeRelease) {
        if (!swiping) return
        swiping = false
        onSwipingChange(false)
        onSwipeEnd(release)
    }

    /** Follows each touch on the row: a swipe slides it, and its release replies, dismisses, or springs it back. */
    suspend fun handle(
        input: PointerInputScope,
        thresholdPx: Float,
        towardStart: Float,
        canReply: Boolean,
        canDismiss: Boolean,
    ) {
        val touchSlopPx = input.viewConfiguration.touchSlop
        input.awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val started = awaitSwipeStart(down.id, touchSlopPx) ?: return@awaitEachGesture
            started.consume()
            var dragPx = 0f
            startSwiping()
            var finished = false
            val lifted = try {
                horizontalDrag(down.id) { change ->
                    // positionChange() reads zero once the change is consumed.
                    val amount = change.positionChange().x
                    change.consume()
                    val wasArmed = chatSwipeRelease(dragPx, thresholdPx) != ChatSwipeRelease.Back
                    dragPx = chatSwipeDrag(dragPx, amount * towardStart, canReply, canDismiss)
                    if (!wasArmed && chatSwipeRelease(dragPx, thresholdPx) != ChatSwipeRelease.Back) onArmed()
                    val target = chatSwipeOffset(dragPx, thresholdPx)
                    scope.launch { offset.snapTo(target) }
                }.also { finished = true }
            } finally {
                // A cancelled drag puts the row back and gives the list its scroll again.
                if (!finished) {
                    endSwiping(ChatSwipeRelease.Back)
                    scope.launch { offset.animateTo(0f, spring()) }
                }
            }
            val release = if (lifted) chatSwipeRelease(dragPx, thresholdPx) else ChatSwipeRelease.Back
            endSwiping(release)
            settle(release, size.width)
        }
    }

    private fun settle(release: ChatSwipeRelease, widthPx: Int) {
        when (release) {
            ChatSwipeRelease.Dismiss -> scope.launch {
                offset.animateTo(-widthPx.toFloat(), spring())
                onDismiss()
            }
            ChatSwipeRelease.Reply -> {
                onReply()
                scope.launch { offset.animateTo(0f, spring()) }
            }
            ChatSwipeRelease.Back -> scope.launch { offset.animateTo(0f, spring()) }
        }
    }
}

/**
 * Slides [content] toward the start under the finger while a reply arrow slides in from the end.
 * Releasing past the threshold calls [onReply]; the row springs back either way. With [onDismiss],
 * the row also follows the finger toward the end, and releasing past the threshold there slides it
 * out and calls [onDismiss]. [onSwipingChange] reports the drag, so the list can hold its scroll still, and
 * [onSwipeEnd] reports how it ended, before the reply or the dismissal it starts.
 */
@Composable
internal fun ChatReplySwipe(
    enabled: Boolean,
    onReply: (() -> Unit)?,
    onSwipingChange: (Boolean) -> Unit,
    onDismiss: (() -> Unit)? = null,
    onSwipeEnd: (ChatSwipeRelease) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { CHAT_REPLY_SWIPE_THRESHOLD.toPx() }
    val circlePx = with(density) { CHAT_REPLY_SWIPE_CIRCLE_SIZE.toPx() }
    val towardStart = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
    val haptic = LocalHapticFeedback.current
    val gestureHints = LocalGestureHints.current
    val scope = rememberCoroutineScope()
    val currentOnReply by rememberUpdatedState(onReply)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val canReply = onReply != null
    val canDismiss = onDismiss != null
    val currentOnSwipingChange by rememberUpdatedState(onSwipingChange)
    val currentOnSwipeEnd by rememberUpdatedState(onSwipeEnd)
    val swipe = remember {
        ChatSwipe(
            scope = scope,
            onSwipingChange = { currentOnSwipingChange(it) },
            onSwipeEnd = { currentOnSwipeEnd(it) },
            onArmed = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
            onReply = {
                gestureHints?.gestureUsed(GestureHint.Reply)
                currentOnReply?.invoke()
            },
            onDismiss = { currentOnDismiss?.invoke() },
        )
    }
    DisposableEffect(Unit) {
        onDispose { swipe.endSwiping(ChatSwipeRelease.Back) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.pointerInput(thresholdPx, towardStart, canReply, canDismiss) {
                        swipe.handle(this, thresholdPx, towardStart, canReply, canDismiss)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        // At rest the arrow sits clipped beyond the end edge; rows not being swiped skip it.
        if (swipe.arrowShown) {
            ReplyArrow(arrowOffsetPx = {
                -towardStart * chatReplyArrowOffset(swipe.offset.value, thresholdPx, circlePx)
            })
        }
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = swipe.offset.value * towardStart
                alpha = chatSwipeDismissAlpha(swipe.offset.value, size.width.roundToInt())
            },
        ) {
            content()
        }
    }
}

/** The reply arrow in its circle at the row's end, shifted by [arrowOffsetPx] as the row slides. */
@Composable
private fun BoxScope.ReplyArrow(arrowOffsetPx: () -> Float) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .clipToBounds()
            .padding(end = (CHAT_REPLY_SWIPE_THRESHOLD - CHAT_REPLY_SWIPE_CIRCLE_SIZE) / 2),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                // A one-line row is lower than the circle: it shrinks to the row height and stays round.
                .sizeIn(maxWidth = CHAT_REPLY_SWIPE_CIRCLE_SIZE, maxHeight = CHAT_REPLY_SWIPE_CIRCLE_SIZE)
                .aspectRatio(1f)
                .graphicsLayer { translationX = arrowOffsetPx() }
                .background(TwitchSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reply_arrow),
                contentDescription = null,
                tint = TwitchText,
                modifier = Modifier.size(CHAT_REPLY_SWIPE_ICON_SIZE),
            )
        }
    }
}
