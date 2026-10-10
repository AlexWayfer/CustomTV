package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The chatter card's banner height. Folded, the banner holds the avatar row with [ChatterHeaderFoldedMargin] above
 * and below it; unfolded, the avatar row keeps [ChatterHeaderMargin] from the bottom.
 */
internal val ChatterHeaderExpandedHeight = 112.dp
internal val ChatterHeaderMargin = 16.dp
internal val ChatterHeaderFoldedMargin = 8.dp

/** The avatar shrinks from [ChatterAvatarSize] to [ChatterAvatarFoldedSize] as the banner folds. */
internal val ChatterAvatarSize = 56.dp
internal val ChatterAvatarFoldedSize = 40.dp

/**
 * Shrinks the chatter card's banner as the card scrolls up, before the content moves, and grows it back once the
 * content is at its top again. A card whose content already fits keeps its banner.
 */
internal class ChatterHeaderCollapse(
    private val expandedPx: Int,
    private val foldedAvatarPx: Int,
    private val foldedMarginPx: Int,
    private val scrollState: ScrollState,
) : NestedScrollConnection {
    var collapsedPx by mutableFloatStateOf(0f)
        private set

    /** How far the banner folds; it fits the avatar row once the names are measured. */
    var rangePx by mutableFloatStateOf(chatterHeaderFoldRangePx(expandedPx, foldedAvatarPx, foldedMarginPx))
        private set

    /** Folds the banner down to the avatar row whose names are [namesHeightPx] tall. */
    fun fitNames(namesHeightPx: Int) {
        rangePx = chatterHeaderFoldRangePx(expandedPx, maxOf(foldedAvatarPx, namesHeightPx), foldedMarginPx)
        collapsedPx = collapsedPx.coerceAtMost(rangePx)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0f && scrollState.canScrollForward) consume(available.y) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y > 0f) consume(available.y) else Offset.Zero

    private fun consume(deltaY: Float): Offset {
        val (next, consumedY) = collapseHeaderBy(collapsedPx, deltaY, rangePx)
        collapsedPx = next
        return Offset(0f, consumedY)
    }
}

@Composable
internal fun rememberChatterHeaderCollapse(scrollState: ScrollState): ChatterHeaderCollapse {
    val density = LocalDensity.current
    return remember(scrollState, density) {
        with(density) {
            ChatterHeaderCollapse(
                expandedPx = ChatterHeaderExpandedHeight.roundToPx(),
                foldedAvatarPx = ChatterAvatarFoldedSize.roundToPx(),
                foldedMarginPx = ChatterHeaderFoldedMargin.roundToPx(),
                scrollState = scrollState,
            )
        }
    }
}

/** How far a banner of [expandedPx] folds to hold a row of [foldedRowPx] with [foldedMarginPx] around it. */
internal fun chatterHeaderFoldRangePx(expandedPx: Int, foldedRowPx: Int, foldedMarginPx: Int): Float =
    (expandedPx - foldedRowPx - 2 * foldedMarginPx).coerceAtLeast(0).toFloat()

/**
 * How far the header is collapsed after a scroll of [deltaY] (negative upward), within 0 and [rangePx], and how much
 * of the scroll that took.
 */
internal fun collapseHeaderBy(collapsedPx: Float, deltaY: Float, rangePx: Float): Pair<Float, Float> {
    val next = (collapsedPx - deltaY).coerceIn(0f, rangePx.coerceAtLeast(0f))
    return next to (collapsedPx - next)
}

/**
 * The collapse the banner shows: the profile's own [collapsedPx] at a [pin] of 0, folded all the way at 1, as on the
 * card's other pages, and in between while it moves.
 */
internal fun pinnedCollapsePx(collapsedPx: Float, rangePx: Float, pin: Float): Float {
    val fraction = pin.coerceIn(0f, 1f)
    return collapsedPx + (rangePx.coerceAtLeast(0f) - collapsedPx) * fraction
}

/** The banner's height in pixels for a collapse of [collapsedPx]; never below zero. */
internal fun chatterHeaderHeightPx(expandedPx: Int, collapsedPx: Float): Int =
    (expandedPx - collapsedPx.roundToInt()).coerceAtLeast(0)

/**
 * How far the avatar row moves down from its unfolded place, so its margin to the banner's bottom narrows to the
 * folded one together with the banner.
 */
internal fun chatterHeaderRowShiftPx(collapsedPx: Float, rangePx: Float, maxShiftPx: Int): Int {
    if (rangePx <= 0f) return 0
    return (maxShiftPx * (collapsedPx / rangePx).coerceIn(0f, 1f)).roundToInt()
}

/** The avatar's size in pixels, from [expandedPx] unfolded down to [foldedPx] once folded; never below zero. */
internal fun chatterAvatarSizePx(collapsedPx: Float, rangePx: Float, expandedPx: Int, foldedPx: Int): Int {
    val fraction = if (rangePx <= 0f) 0f else (collapsedPx / rangePx).coerceIn(0f, 1f)
    return (expandedPx - (expandedPx - foldedPx) * fraction).roundToInt().coerceAtLeast(0)
}
