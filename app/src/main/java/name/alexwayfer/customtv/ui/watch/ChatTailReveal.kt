package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.exp

/** Pixels/ms. Preserve velocity across rows; ease changes in distance to the live edge. */
internal fun chatTailVelocity(previous: Float, remaining: Float, elapsedMillis: Float, baseMillis: Int): Float {
    if (remaining <= 0f) return 0f
    val target = remaining * 3f / baseMillis
    val blend = 1f - exp(-elapsedMillis / 80f)
    return previous + (target - previous) * blend
}

/**
 * Whether the reveal moves the list on this frame: at most 60 steps a second, so a 120 Hz display redraws the chat on
 * every other frame. The step length follows the elapsed time, so the speed stays the same.
 */
internal fun chatTailStepDue(sinceLastStepNanos: Long): Boolean = sinceLastStepNanos >= CHAT_TAIL_MIN_STEP_NANOS

// Between a 120 Hz frame (8.3 ms) and a 60 Hz one (16.7 ms), so vsync jitter does not drop a 60 Hz step.
private const val CHAT_TAIL_MIN_STEP_NANOS = 12_500_000L

/** One scroll mutation for the whole burst, with no per-row pauses or easing restarts. */
internal suspend fun revealChatTail(
    listState: LazyListState,
    baseMillis: Int,
    initialPending: Int,
    latestMessageId: () -> String?,
    shouldContinue: () -> Boolean,
): String? {
    var revealedTail: String? = null
    listState.scroll {
        var previousFrame = withFrameNanos { it }
        val initialTail = latestMessageId()
        var continuous = initialPending > 1
        var elapsed = 0f
        var initialDistance: Float? = null
        var singleScrolled = 0f
        var velocity = 0f
        while (shouldContinue()) {
            val frame = withFrameNanos { it }
            // A frame without a scroll step draws nothing new, so the screen stays at 60 Hz while chat moves.
            if (!chatTailStepDue(frame - previousFrame)) continue
            val deltaMillis = ((frame - previousFrame) / 1_000_000f).coerceIn(0.1f, 32f)
            previousFrame = frame
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull() ?: continue
            val tailId = last.key as? String
            // Do not acknowledge an arrival before LazyColumn has laid it out.
            if (!listState.canScrollForward && tailId == latestMessageId()) {
                revealedTail = tailId
                break
            }
            val unseen = (layout.totalItemsCount - last.index - 1).coerceAtLeast(0)
            val averageSize = layout.visibleItemsInfo.sumOf { it.size }.toFloat() / layout.visibleItemsInfo.size
            val remaining = (last.offset + last.size - layout.viewportEndOffset + unseen * averageSize)
                .coerceAtLeast(0f)
            if (initialDistance == null) initialDistance = remaining
            continuous = continuous || latestMessageId() != initialTail
            val distance = if (continuous) {
                velocity = chatTailVelocity(velocity, remaining, deltaMillis, baseMillis)
                if (remaining < 0.5f) remaining else minOf(remaining, velocity * deltaMillis)
            } else {
                elapsed += deltaMillis
                val progress = (elapsed / baseMillis).coerceAtMost(1f)
                val target = initialDistance * FastOutSlowInEasing.transform(progress)
                if (progress == 1f) remaining else (target - singleScrolled).coerceAtLeast(0f)
            }
            val consumed = scrollBy(distance)
            if (!continuous) {
                singleScrolled += consumed
                velocity = consumed / deltaMillis
            }
        }
    }
    return revealedTail
}
