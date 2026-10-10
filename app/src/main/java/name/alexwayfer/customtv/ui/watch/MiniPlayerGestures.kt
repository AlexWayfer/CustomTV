package name.alexwayfer.customtv.ui.watch

import kotlin.math.abs
import kotlin.math.roundToInt

internal data class MiniPlayerBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal data class MiniPlayerVisualState(
    val swipeAlpha: Float,
    val chromeAlpha: Float,
    val controlsAlpha: Float,
)

internal fun miniPlayerDragProgress(
    offsetY: Float,
    screenHeightPx: Float,
    fromMiniPlayer: Boolean,
): Float {
    val range = (screenHeightPx * 0.55f).coerceAtLeast(280f)
    val downwardProgress = { offset: Float ->
        (offset.coerceAtLeast(0f) / range).coerceIn(0f, 1f)
    }
    return if (fromMiniPlayer) {
        (1f - downwardProgress(-offsetY)).coerceIn(0f, 1f)
    } else {
        downwardProgress(offsetY)
    }
}

internal fun shouldStayMinimized(
    progress: Float,
    velocityY: Float,
    startedMinimized: Boolean,
): Boolean = if (startedMinimized) {
    progress > 0.55f && velocityY > -2800f
} else {
    progress > MINIMIZE_DRAG_PROGRESS || velocityY > MINIMIZE_FLING_VELOCITY
}

/** How far down an expanded player is dragged, as drag progress, before it minimizes. */
internal const val MINIMIZE_DRAG_PROGRESS = 0.42f

/** How fast a drag down minimizes an expanded player however short it is, in pixels per second. */
internal const val MINIMIZE_FLING_VELOCITY = 3200f

internal fun shouldCloseMiniPlayer(
    offsetX: Float,
    velocityX: Float,
    playerWidthPx: Float,
): Boolean {
    val range = playerWidthPx.coerceAtLeast(1f)
    val sameDirection = offsetX * velocityX > 0f
    return abs(offsetX) > range * 0.38f ||
        (sameDirection && abs(velocityX) > 1100f)
}

internal fun miniPlayerSwipeAlpha(offsetX: Float, playerWidthPx: Float): Float =
    (1f - abs(offsetX) / (playerWidthPx * 0.45f).coerceAtLeast(1f)).coerceIn(0f, 1f)

internal fun miniPlayerVisualState(
    progress: Float,
    swipeOffsetX: Float,
    playerWidthPx: Float,
    expanding: Boolean,
): MiniPlayerVisualState {
    val chromeRevealUntil = if (expanding) 0.28f else 0.1f
    return MiniPlayerVisualState(
        swipeAlpha = miniPlayerSwipeAlpha(swipeOffsetX, playerWidthPx),
        chromeAlpha = ((chromeRevealUntil - progress) / chromeRevealUntil).coerceIn(0f, 1f),
        controlsAlpha = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f),
    )
}

internal fun miniPlayerBounds(
    fullWidthPx: Float,
    fullHeightPx: Float,
    miniWidthPx: Float,
    miniPaddingPx: Float,
    statusBarTopPx: Float,
    navigationBarBottomPx: Float,
    progress: Float,
): MiniPlayerBounds {
    fun lerp(start: Float, stop: Float): Float = start * (1f - progress) + stop * progress

    val miniHeightPx = miniWidthPx * 9f / 16f
    val width = lerp(fullWidthPx, miniWidthPx).roundToInt()
    val height = (width * 9f / 16f).roundToInt()
    val x = (fullWidthPx - lerp(0f, miniPaddingPx) - width).roundToInt()
    val y = lerp(
        statusBarTopPx,
        fullHeightPx - navigationBarBottomPx - miniPaddingPx - miniHeightPx,
    ).roundToInt()
    return MiniPlayerBounds(x = x, y = y, width = width, height = height)
}

/**
 * A tap on the mini player, or on a player almost in the corner, expands it. That tap is not passed
 * on to the player, so it does not toggle the player's own controls on the way.
 */
internal fun playerTapExpands(inPictureInPicture: Boolean, compactSettled: Boolean, progress: Float): Boolean =
    !inPictureInPicture && (compactSettled || progress > 0.85f)

/**
 * The player's controls hide as soon as a full player starts into the corner, not once it gets there. A mini player
 * that animates, such as when it is swiped away, has its controls hidden already.
 */
internal fun controlsHideAsCollapseStarts(collapsing: Boolean, minimized: Boolean): Boolean = collapsing && !minimized
