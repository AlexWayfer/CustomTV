package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.GestureHint
import name.alexwayfer.customtv.ui.LocalGestureHints

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

/**
 * What a swipe does: [onArmed] ticks when the travel crosses the threshold, and a release replies, copies, or
 * dismisses.
 */
internal data class ChatSwipeActions(
    val onArmed: () -> Unit,
    val onReply: () -> Unit,
    val onCopy: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * The row's slide under the finger and what releasing it does. The callbacks read the latest the row was composed
 * with; [onSwipeEnd] reports how the swipe ended, and [onOffsetChange] follows each step of the slide.
 */
internal class ChatSwipe(
    private val scope: CoroutineScope,
    private val onSwipingChange: (Boolean) -> Unit,
    private val onSwipeEnd: (ChatSwipeRelease) -> Unit,
    private val onOffsetChange: (Float) -> Unit,
    private val actions: ChatSwipeActions,
) {
    val offset = Animatable(0f)
    val replyArrowShown by derivedStateOf { offset.value > 0f }
    val towardEndShown by derivedStateOf { offset.value < 0f }
    private var swiping = false

    private suspend fun snapTo(target: Float) {
        offset.snapTo(target)
        onOffsetChange(target)
    }

    private suspend fun springTo(target: Float) {
        offset.animateTo(target, spring()) { onOffsetChange(value) }
    }

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

    /** A row that leaves ends its swipe and reports rest. */
    fun leave() {
        endSwiping(ChatSwipeRelease.Back)
        onOffsetChange(0f)
    }

    /**
     * Follows each touch on the row: a swipe slides it, and its release replies, copies, dismisses, or springs it
     * back.
     */
    suspend fun handle(
        input: PointerInputScope,
        thresholdPx: Float,
        towardStart: Float,
        canReply: Boolean,
        towardEnd: ChatSwipeTowardEnd,
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
                    val wasArmed = chatSwipeRelease(dragPx, thresholdPx, towardEnd) != ChatSwipeRelease.Back
                    dragPx = chatSwipeDrag(dragPx, amount * towardStart, canReply, towardEnd)
                    val armed = chatSwipeRelease(dragPx, thresholdPx, towardEnd) != ChatSwipeRelease.Back
                    if (!wasArmed && armed) actions.onArmed()
                    val target = chatSwipeOffset(dragPx, thresholdPx, towardEnd)
                    scope.launch { snapTo(target) }
                }.also { finished = true }
            } finally {
                // A cancelled drag puts the row back and gives the list its scroll again.
                if (!finished) {
                    endSwiping(ChatSwipeRelease.Back)
                    scope.launch { springTo(0f) }
                }
            }
            val release = if (lifted) chatSwipeRelease(dragPx, thresholdPx, towardEnd) else ChatSwipeRelease.Back
            endSwiping(release)
            settle(release, size.width)
        }
    }

    private fun settle(release: ChatSwipeRelease, widthPx: Int) {
        when (release) {
            ChatSwipeRelease.Dismiss -> scope.launch {
                springTo(-widthPx.toFloat())
                actions.onDismiss()
            }
            ChatSwipeRelease.Reply -> {
                actions.onReply()
                scope.launch { springTo(0f) }
            }
            ChatSwipeRelease.Copy -> {
                actions.onCopy()
                scope.launch { springTo(0f) }
            }
            ChatSwipeRelease.Back -> scope.launch { springTo(0f) }
        }
    }
}

/** The row's [ChatSwipe], kept while the row stays; it calls the latest callbacks the row was composed with. */
@Composable
internal fun rememberChatSwipe(
    onReply: (() -> Unit)?,
    onSwipingChange: (Boolean) -> Unit,
    onDismiss: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onSwipeEnd: (ChatSwipeRelease) -> Unit,
    onOffsetChange: (Float) -> Unit,
): ChatSwipe {
    val haptic = LocalHapticFeedback.current
    val gestureHints = LocalGestureHints.current
    val scope = rememberCoroutineScope()
    val currentOnReply by rememberUpdatedState(onReply)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnCopy by rememberUpdatedState(onCopy)
    val currentOnSwipingChange by rememberUpdatedState(onSwipingChange)
    val currentOnSwipeEnd by rememberUpdatedState(onSwipeEnd)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val swipe = remember {
        ChatSwipe(
            scope = scope,
            onSwipingChange = { currentOnSwipingChange(it) },
            onSwipeEnd = { currentOnSwipeEnd(it) },
            onOffsetChange = { currentOnOffsetChange(it) },
            actions = ChatSwipeActions(
                onArmed = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                onReply = {
                    gestureHints?.gestureUsed(GestureHint.Reply)
                    currentOnReply?.invoke()
                },
                onCopy = { currentOnCopy?.invoke() },
                onDismiss = { currentOnDismiss?.invoke() },
            ),
        )
    }
    DisposableEffect(Unit) {
        onDispose { swipe.leave() }
    }
    return swipe
}
