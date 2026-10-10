package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.FullscreenChatSide
import kotlin.math.abs

internal enum class FullscreenChatSwipeOutcome { Stay, Hide, MoveAcross }

/**
 * A swipe on the full screen chat toward its own edge hides it; toward the other side, it moves the chat there. Either
 * takes a third of the chat's width or a fling; anything less springs back.
 */
internal fun fullscreenChatSwipeOutcome(
    offsetX: Float,
    velocityX: Float,
    chatOnLeft: Boolean,
    chatWidthPx: Int,
): FullscreenChatSwipeOutcome {
    val outward = if (chatOnLeft) -1f else 1f
    val along = offsetX * outward
    val speed = velocityX * outward
    val threshold = chatWidthPx * FULLSCREEN_CHAT_SWIPE_SHARE
    return when {
        along > 0f && (along > threshold || speed > MINIMIZE_FLING_VELOCITY) -> FullscreenChatSwipeOutcome.Hide
        along < 0f && (-along > threshold || -speed > MINIMIZE_FLING_VELOCITY) -> FullscreenChatSwipeOutcome.MoveAcross
        else -> FullscreenChatSwipeOutcome.Stay
    }
}

/** The chat follows the finger out to its own width past the edge, or across to the other side. */
internal fun fullscreenChatSwipeOffset(offsetX: Float, chatOnLeft: Boolean, chatWidthPx: Int, travelPx: Int): Float {
    val outward = chatWidthPx.coerceAtLeast(0).toFloat()
    val across = travelPx.coerceAtLeast(0).toFloat()
    return if (chatOnLeft) offsetX.coerceIn(-outward, across) else offsetX.coerceIn(-across, outward)
}

/** The chat fades as it leaves past its own edge, and stays opaque moving across. */
internal fun fullscreenChatSwipeAlpha(offsetX: Float, chatOnLeft: Boolean, chatWidthPx: Int): Float {
    if (chatWidthPx <= 0) return 1f
    val outwardPx = if (chatOnLeft) -offsetX else offsetX
    return (1f - outwardPx.coerceAtLeast(0f) / chatWidthPx).coerceIn(0f, 1f)
}

/**
 * Beside a column chat the video follows the swipe: half the way toward the chat's edge as the chat leaves, since it
 * centers in the whole width once the chat is gone, or across by the chat's width as the chat changes sides. Over the
 * video, the chat moves alone.
 */
internal fun fullscreenPlayerSwipeShiftPx(offsetX: Float, bounds: FullscreenChatBounds?, travelPx: Int): Float {
    if (bounds == null || bounds.overPlayer || offsetX == 0f) return 0f
    val outward = if (bounds.chatOnLeft) offsetX < 0f else offsetX > 0f
    return if (outward) {
        offsetX / 2f
    } else if (travelPx > 0) {
        -offsetX * bounds.chatWidth / travelPx
    } else {
        0f
    }
}

/** A swipe left on the video brings the hidden chat in from the right, a swipe right from the left. */
internal fun fullscreenChatRevealSide(fingerX: Float): FullscreenChatSide =
    if (fingerX < 0f) FullscreenChatSide.Right else FullscreenChatSide.Left

/** How far past its edge the coming chat still is: all its width at first, none once the finger has pulled it in. */
internal fun fullscreenChatRevealOffset(side: FullscreenChatSide, fingerX: Float, chatWidthPx: Int): Float {
    val width = chatWidthPx.coerceAtLeast(0).toFloat()
    return if (side == FullscreenChatSide.Right) (width + fingerX).coerceIn(0f, width) else (-width + fingerX).coerceIn(-width, 0f)
}

/** The coming chat stays once a third of it is in or it is flung inward; else it slides back out. */
internal fun fullscreenChatRevealShows(side: FullscreenChatSide, offsetX: Float, velocityX: Float, chatWidthPx: Int): Boolean {
    val inward = if (side == FullscreenChatSide.Right) -1f else 1f
    val shownPx = chatWidthPx - abs(offsetX)
    return shownPx > chatWidthPx * FULLSCREEN_CHAT_SWIPE_SHARE || velocityX * inward > MINIMIZE_FLING_VELOCITY
}

/** Where a swipe on the full screen chat has moved it; animates the rest of the way once released. */
@Stable
internal class FullscreenChatSwipeState(private val scope: CoroutineScope) {
    var offsetX by mutableFloatStateOf(0f)
        private set

    /** The side a hidden chat comes in from while a swipe or the Chat button brings it; null otherwise. */
    var revealSide by mutableStateOf<FullscreenChatSide?>(null)
        private set
    private var settleJob: Job? = null

    /** Follows a swipe on the video that brings the hidden chat in; [fingerX] is the distance from the start. */
    fun revealDrag(fingerX: Float, chatWidthPx: Int) {
        settleJob?.cancel()
        val side = revealSide ?: fullscreenChatRevealSide(fingerX).also { revealSide = it }
        offsetX = fullscreenChatRevealOffset(side, fingerX, chatWidthPx)
    }

    /** Ends that swipe: slides the chat the rest of the way in and calls [onShow], or back out. */
    fun revealRelease(velocityX: Float, chatWidthPx: Int, onShow: (FullscreenChatSide) -> Unit) {
        val side = revealSide ?: return
        val shows = fullscreenChatRevealShows(side, offsetX, velocityX, chatWidthPx)
        settleJob = scope.launch {
            if (shows) {
                slideTo(0f)
                onShow(side)
            } else {
                slideTo(fullscreenChatRevealOffset(side, 0f, chatWidthPx))
            }
            revealSide = null
            offsetX = 0f
        }
    }

    /** Brings the hidden chat in from [side], as the Chat button does. */
    fun reveal(side: FullscreenChatSide, chatWidthPx: Int, onShow: (FullscreenChatSide) -> Unit) {
        settleJob?.cancel()
        revealSide = side
        offsetX = fullscreenChatRevealOffset(side, 0f, chatWidthPx)
        settleJob = scope.launch {
            slideTo(0f)
            onShow(side)
            revealSide = null
            offsetX = 0f
        }
    }

    fun drag(deltaX: Float, chatOnLeft: Boolean, chatWidthPx: Int, travelPx: Int) {
        settleJob?.cancel()
        offsetX = fullscreenChatSwipeOffset(offsetX + deltaX, chatOnLeft, chatWidthPx, travelPx)
    }

    /** Hides the chat or moves it across past the threshold, sliding it the rest of the way first; else springs back. */
    fun release(
        velocityX: Float,
        chatOnLeft: Boolean,
        chatWidthPx: Int,
        travelPx: Int,
        onHide: () -> Unit,
        onMoveAcross: () -> Unit,
    ) {
        val outcome = fullscreenChatSwipeOutcome(offsetX, velocityX, chatOnLeft, chatWidthPx)
        val outward = if (chatOnLeft) -chatWidthPx.toFloat() else chatWidthPx.toFloat()
        val across = if (chatOnLeft) travelPx.toFloat() else -travelPx.toFloat()
        settleJob = scope.launch {
            when (outcome) {
                FullscreenChatSwipeOutcome.Stay -> slideTo(0f)
                FullscreenChatSwipeOutcome.Hide -> {
                    slideTo(outward)
                    onHide()
                    offsetX = 0f
                }
                FullscreenChatSwipeOutcome.MoveAcross -> {
                    slideTo(across)
                    onMoveAcross()
                    offsetX = 0f
                }
            }
        }
    }

    fun cancel() {
        settleJob?.cancel()
        settleJob = scope.launch { slideTo(0f) }
    }

    private suspend fun slideTo(target: Float) {
        animate(offsetX, target, animationSpec = ChatSwipeSettleTween) { value, _ -> offsetX = value }
    }
}

/** Follows sideways swipes on the full screen chat and moves or fades it along. */
internal fun Modifier.fullscreenChatSwipe(
    swipe: FullscreenChatSwipeState,
    chatOnLeft: Boolean,
    chatWidthPx: Int,
    travelPx: Int,
    onHide: () -> Unit,
    onMoveAcross: () -> Unit,
): Modifier = pointerInput(swipe, chatOnLeft, chatWidthPx, travelPx) {
    val velocity = VelocityTracker()
    try {
        detectHorizontalDragGestures(
            onDragStart = { velocity.resetTracking() },
            onDragEnd = {
                swipe.release(
                    velocityX = velocity.calculateVelocity().x,
                    chatOnLeft = chatOnLeft,
                    chatWidthPx = chatWidthPx,
                    travelPx = travelPx,
                    onHide = onHide,
                    onMoveAcross = onMoveAcross,
                )
            },
            onDragCancel = swipe::cancel,
        ) { change, dragAmount ->
            velocity.addPosition(change.uptimeMillis, change.position)
            change.consume()
            swipe.drag(dragAmount, chatOnLeft, chatWidthPx, travelPx)
        }
    } catch (e: CancellationException) {
        swipe.cancel()
        throw e
    }
}.graphicsLayer {
    translationX = swipe.offsetX
    alpha = fullscreenChatSwipeAlpha(swipe.offsetX, chatOnLeft, chatWidthPx)
}

/** The share of the chat's width a swipe takes to hide or move it. */
internal const val FULLSCREEN_CHAT_SWIPE_SHARE = 1f / 3f

internal val ChatSwipeSettleTween =tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
