package name.alexwayfer.customtv.ui.watch

import kotlin.math.abs

/** Which way the app holds the screen while the player goes in or out of full screen. */
internal enum class FullscreenLock {
    /** The screen follows the device; landscape means full screen. */
    None,

    /** Held in landscape after the full screen button, until the device itself turns to landscape. */
    Landscape,

    /** Held in portrait after leaving full screen in landscape, until the device itself turns to portrait. */
    Portrait,
}

internal enum class DeviceTurn { Portrait, Landscape }

/**
 * The player fills the screen while it is expanded and the screen is in landscape. The lock only turns the screen:
 * the layout waits for the turn, so the player never fills a portrait screen or keeps the portrait layout in
 * landscape on the way.
 */
internal fun playerFullscreen(expanded: Boolean, displayLandscape: Boolean): Boolean = expanded && displayLandscape

/** Leaving full screen while the screen is in landscape holds it in portrait, or it would enter again at once. */
internal fun lockAfterFullscreenExit(displayLandscape: Boolean): FullscreenLock =
    if (displayLandscape) FullscreenLock.Portrait else FullscreenLock.None

/** A minimized player does not keep the app in landscape. */
internal fun lockAfterMinimize(lock: FullscreenLock): FullscreenLock =
    if (lock == FullscreenLock.Landscape) FullscreenLock.None else lock

/**
 * Hands the screen back to the device once the device turns the way the app holds it, so turning it back
 * enters or leaves full screen. With auto-rotate off, a landscape hold stays: the screen would turn back.
 */
internal fun lockAfterDeviceTurn(lock: FullscreenLock, turn: DeviceTurn?, autoRotate: () -> Boolean): FullscreenLock =
    when (lock) {
        FullscreenLock.Portrait if turn == DeviceTurn.Portrait -> FullscreenLock.None
        FullscreenLock.Landscape if turn == DeviceTurn.Landscape && autoRotate() -> FullscreenLock.None
        else -> lock
    }

/**
 * How the device is turned, from `OrientationEventListener` degrees; null when it lies flat or sits between
 * the two, so a tilt halfway does not release the hold.
 */
internal fun deviceTurn(degrees: Int): DeviceTurn? {
    if (degrees < 0) return null
    val fromUpright = degrees % 180
    return when {
        fromUpright <= DEVICE_TURN_TOLERANCE || fromUpright >= 180 - DEVICE_TURN_TOLERANCE -> DeviceTurn.Portrait
        fromUpright in 90 - DEVICE_TURN_TOLERANCE..90 + DEVICE_TURN_TOLERANCE -> DeviceTurn.Landscape
        else -> null
    }
}

/** Which gesture a vertical drag on the player is, from its first move. */
internal enum class FullscreenDrag(val ownsDrag: Boolean) {
    /** No drag yet. */
    None(false),

    /** The minimize gesture's drag. */
    Other(false),

    /** A drag down in full screen. */
    Exit(true),

    /** A drag up on the expanded player. */
    Enter(true),
}

internal fun fullscreenDragGesture(active: Boolean, canEnter: Boolean, offsetY: Float): FullscreenDrag = when {
    active -> FullscreenDrag.Exit
    canEnter && offsetY < 0f -> FullscreenDrag.Enter
    else -> FullscreenDrag.Other
}

/** The player follows the finger at part of its pace, and only in the gesture's direction. */
internal fun fullscreenDragOffset(gesture: FullscreenDrag, offsetY: Float): Float = when (gesture) {
    FullscreenDrag.Exit -> offsetY.coerceAtLeast(0f) * DRAG_FOLLOW
    FullscreenDrag.Enter -> offsetY.coerceAtMost(0f) * DRAG_FOLLOW
    FullscreenDrag.None, FullscreenDrag.Other -> 0f
}

/**
 * A drag down leaves full screen where the same drag would minimize the expanded player; a drag up enters it at
 * half that distance. A fling of the minimize speed toggles either way.
 */
internal fun fullscreenDragToggles(
    gesture: FullscreenDrag,
    offsetY: Float,
    velocityY: Float,
    screenHeightPx: Float,
): Boolean = when (gesture) {
    FullscreenDrag.Exit -> fullscreenDragProgress(offsetY, screenHeightPx) > EXIT_DRAG_PROGRESS ||
        velocityY > MINIMIZE_FLING_VELOCITY
    FullscreenDrag.Enter -> fullscreenDragProgress(-offsetY, screenHeightPx) > ENTER_DRAG_PROGRESS ||
        -velocityY > MINIMIZE_FLING_VELOCITY
    FullscreenDrag.None, FullscreenDrag.Other -> false
}

private fun fullscreenDragProgress(fingerDistance: Float, screenHeightPx: Float): Float =
    miniPlayerDragProgress(fingerDistance, screenHeightPx, fromMiniPlayer = false)

/**
 * The player shrinks as it is dragged down out of full screen and grows as it is dragged up into it, reaching
 * its full change at the distance that toggles full screen.
 */
internal fun fullscreenDragScale(dragOffsetY: Float, screenHeightPx: Float): Float {
    val progress = fullscreenDragProgress(abs(dragOffsetY) / DRAG_FOLLOW, screenHeightPx)
    return if (dragOffsetY >= 0f) {
        1f - DRAG_SCALE * (progress / EXIT_DRAG_PROGRESS).coerceAtMost(1f)
    } else {
        1f + DRAG_SCALE * (progress / ENTER_DRAG_PROGRESS).coerceAtMost(1f)
    }
}

private const val EXIT_DRAG_PROGRESS = MINIMIZE_DRAG_PROGRESS
private const val ENTER_DRAG_PROGRESS = MINIMIZE_DRAG_PROGRESS / 2

private const val DEVICE_TURN_TOLERANCE = 30
private const val DRAG_FOLLOW = 0.5f
private const val DRAG_SCALE = 0.1f
