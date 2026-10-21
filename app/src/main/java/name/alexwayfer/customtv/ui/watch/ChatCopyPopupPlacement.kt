package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

private const val MARGIN = 8

internal data class ChatCopyPopupPlacement(val offset: IntOffset, val above: Boolean)

/**
 * Places the copy popup next to the held message, [MARGIN] away from it and from the window edges.
 * [placeAbove] keeps the side chosen on the first pass, so the popup does not jump while it resizes.
 * A window smaller than the popup puts it at the margin instead of failing.
 */
internal fun chatCopyPopupPlacement(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupSize: IntSize,
    placeAbove: Boolean?,
): ChatCopyPopupPlacement {
    val x = if (layoutDirection == LayoutDirection.Ltr) {
        anchorBounds.right - popupSize.width - MARGIN
    } else {
        anchorBounds.left + MARGIN
    }
    val above = anchorBounds.top - popupSize.height - MARGIN
    val below = anchorBounds.bottom + MARGIN
    val canPlaceBelow = below + popupSize.height <= windowSize.height - MARGIN
    val canPlaceAbove = above >= MARGIN
    val placedAbove = placeAbove ?: (!canPlaceBelow && canPlaceAbove)
    val y = if (placedAbove) above else below
    return ChatCopyPopupPlacement(
        offset = IntOffset(
            x.coerceInWindow(windowSize.width, popupSize.width),
            y.coerceInWindow(windowSize.height, popupSize.height),
        ),
        above = placedAbove,
    )
}

private fun Int.coerceInWindow(window: Int, popup: Int): Int =
    coerceIn(MARGIN, maxOf(MARGIN, window - popup - MARGIN))
