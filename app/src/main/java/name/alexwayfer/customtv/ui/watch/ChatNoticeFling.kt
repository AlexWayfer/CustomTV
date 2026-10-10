package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The notice page a fling settles on. [position] counts pages from the first, so 1.3 is a third of
 * the way from the second page to the third. A fling faster than [minFlingVelocity] moves to the
 * next page edge in its direction and no further; a slower release settles on the nearest page.
 */
internal fun noticePageAfterFling(
    position: Float,
    velocity: Float,
    minFlingVelocity: Float,
    pageCount: Int,
): Int {
    if (pageCount <= 0) return 0
    val page = when {
        abs(velocity) < minFlingVelocity -> position.roundToInt()
        velocity > 0f -> ceil(position).toInt()
        else -> floor(position).toInt()
    }
    return page.coerceIn(0, pageCount - 1)
}

/** Lets a fling move the notices one page at a time, like a Compose pager, instead of coasting past pages. */
internal class ChatNoticeFlingBehavior(
    private val scroll: ScrollState,
    private val pageWidthPx: Int,
    private val pageCount: Int,
    private val minFlingVelocity: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (pageWidthPx <= 0) return initialVelocity
        val position = scroll.value.toFloat() / pageWidthPx
        val target = noticePageAfterFling(position, initialVelocity, minFlingVelocity, pageCount) * pageWidthPx
        var previous = scroll.value.toFloat()
        AnimationState(initialValue = previous, initialVelocity = initialVelocity).animateTo(target.toFloat()) {
            val consumed = scrollBy(value - previous)
            previous += consumed
            if (abs(value - previous) > 0.5f) cancelAnimation()
        }
        return 0f
    }
}
