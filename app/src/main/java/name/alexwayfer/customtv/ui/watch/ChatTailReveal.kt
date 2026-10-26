package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.ScrollScope
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

/** Less than this is left to the newest row, so a gliding burst takes the rest at once. */
private const val CHAT_TAIL_SNAP_PX = 0.5f

/**
 * One glide for the whole burst, with no per-row pauses or easing restarts. Each step is its own short scroll
 * mutation: between frames the list is not scrolling, so a touch reaches the rows instead of starting a drag that
 * stops the list, as a touch during a fling does.
 */
internal suspend fun revealChatTail(
    listState: LazyListState,
    baseMillis: Int,
    initialPending: Int,
    latestMessageId: () -> String?,
    shouldContinue: () -> Boolean,
): String? {
    var previousFrame = withFrameNanos { it }
    val steps = ChatTailSteps(listState, baseMillis, continuous = initialPending > 1, latestMessageId)
    var revealed = false
    while (!revealed && shouldContinue()) {
        val frame = withFrameNanos { it }
        // A step on every display frame; its length follows the elapsed time, so the speed is the same at any rate.
        val deltaMillis = ((frame - previousFrame) / 1_000_000f).coerceIn(0.1f, 32f)
        previousFrame = frame
        listState.scroll { revealed = steps.scrollStep(this, deltaMillis) }
    }
    return steps.revealedTail
}

/** The reveal's progress across frames: one burst glides at a steady pace, a single row eases in. */
private class ChatTailSteps(
    private val listState: LazyListState,
    private val baseMillis: Int,
    private var continuous: Boolean,
    private val latestMessageId: () -> String?,
) {
    private val initialTail = latestMessageId()
    private var elapsed = 0f
    private var initialDistance: Float? = null
    private var singleScrolled = 0f
    private var velocity = 0f

    /** The newest row once it is in sight. */
    var revealedTail: String? = null
        private set

    /** Scrolls one frame's step; true once the newest row is in sight. */
    fun scrollStep(scroll: ScrollScope, deltaMillis: Float): Boolean {
        val layout = listState.layoutInfo
        val last = layout.visibleItemsInfo.lastOrNull() ?: return false
        val tailId = last.key as? String
        // Do not acknowledge an arrival before LazyColumn has laid it out.
        if (!listState.canScrollForward && tailId == latestMessageId()) {
            revealedTail = tailId
            return true
        }
        val unseen = (layout.totalItemsCount - last.index - 1).coerceAtLeast(0)
        val averageSize = layout.visibleItemsInfo.sumOf { it.size }.toFloat() / layout.visibleItemsInfo.size
        val remaining = (last.offset + last.size - layout.viewportEndOffset + unseen * averageSize)
            .coerceAtLeast(0f)
        continuous = continuous || latestMessageId() != initialTail
        val consumed = scroll.scrollBy(distance(remaining, deltaMillis))
        if (!continuous) {
            singleScrolled += consumed
            velocity = consumed / deltaMillis
        }
        return false
    }

    private fun distance(remaining: Float, deltaMillis: Float): Float {
        val startDistance = initialDistance ?: remaining.also { initialDistance = it }
        if (continuous) {
            velocity = chatTailVelocity(velocity, remaining, deltaMillis, baseMillis)
            return if (remaining < CHAT_TAIL_SNAP_PX) remaining else minOf(remaining, velocity * deltaMillis)
        }
        elapsed += deltaMillis
        val progress = (elapsed / baseMillis).coerceAtMost(1f)
        val target = startDistance * FastOutSlowInEasing.transform(progress)
        return if (progress == 1f) remaining else (target - singleScrolled).coerceAtLeast(0f)
    }
}
