package name.alexwayfer.customtv.ui.watch

import kotlin.math.abs
import kotlin.math.roundToInt

// A minimized player dragged up stays in the corner unless it rises past this, or is flung up faster.
private const val STAY_MINIMIZED_PROGRESS = 0.55f
private const val EXPAND_FLING_VELOCITY = 2800f

// A mini player swiped aside closes past this share of its width, or when flung that way faster.
private const val CLOSE_SWIPE_SHARE = 0.38f
private const val CLOSE_FLING_VELOCITY = 1100f

/** The share of its width a swiped mini player goes before it is gone from sight. */
private const val SWIPE_FADE_SHARE = 0.45f

/** A player this close to the corner is expanded by a tap. */
private const val TAP_EXPANDS_PROGRESS = 0.85f

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
    progress > STAY_MINIMIZED_PROGRESS && velocityY > -EXPAND_FLING_VELOCITY
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
    return abs(offsetX) > range * CLOSE_SWIPE_SHARE ||
        (sameDirection && abs(velocityX) > CLOSE_FLING_VELOCITY)
}

internal fun miniPlayerSwipeAlpha(offsetX: Float, playerWidthPx: Float): Float =
    (1f - abs(offsetX) / (playerWidthPx * SWIPE_FADE_SHARE).coerceAtLeast(1f)).coerceIn(0f, 1f)

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

/**
 * The mini player's width and its gap from the screen's edges, and the system bars the player keeps clear of: the
 * full one sits under the status bar, the mini one above the navigation bar.
 */
internal data class MiniPlayerDock(
    val miniWidthPx: Float,
    val miniPaddingPx: Float,
    val statusBarTopPx: Float,
    val navigationBarBottomPx: Float,
)

internal fun miniPlayerBounds(
    fullWidthPx: Float,
    fullHeightPx: Float,
    dock: MiniPlayerDock,
    progress: Float,
): MiniPlayerBounds {
    fun lerp(start: Float, stop: Float): Float = start * (1f - progress) + stop * progress

    val miniHeightPx = dock.miniWidthPx * 9f / 16f
    val width = lerp(fullWidthPx, dock.miniWidthPx).roundToInt()
    val height = (width * 9f / 16f).roundToInt()
    val x = (fullWidthPx - lerp(0f, dock.miniPaddingPx) - width).roundToInt()
    val y = lerp(
        dock.statusBarTopPx,
        fullHeightPx - dock.navigationBarBottomPx - dock.miniPaddingPx - miniHeightPx,
    ).roundToInt()
    return MiniPlayerBounds(x = x, y = y, width = width, height = height)
}

/**
 * A tap on the mini player, or on a player almost in the corner, expands it. That tap is not passed
 * on to the player, so it does not toggle the player's own controls on the way.
 */
internal fun playerTapExpands(inPictureInPicture: Boolean, compactSettled: Boolean, progress: Float): Boolean =
    !inPictureInPicture && (compactSettled || progress > TAP_EXPANDS_PROGRESS)

/**
 * The player's controls hide as soon as a full player starts into the corner, not once it gets there. A mini player
 * that animates, such as when it is swiped away, has its controls hidden already.
 */
internal fun controlsHideAsCollapseStarts(collapsing: Boolean, minimized: Boolean): Boolean = collapsing && !minimized
