package name.alexwayfer.customtv.player

import kotlin.math.roundToInt

/** How tall the player's control bar is on screen, from its share of the page and the web view's height. */
internal fun controlBarHeightPx(fraction: Double, viewHeightPx: Int): Int =
    (fraction.coerceIn(0.0, 1.0) * viewHeightPx.coerceAtLeast(0)).roundToInt()

/**
 * A touch on the player controls while they show belongs to the page from its start to its end, such as a press on a
 * recording's seek bar that drifts up before it moves sideways: the player takes no swipe from it, in any direction.
 * Hidden controls only show on that touch, so the touch can still swipe the player.
 */
internal fun touchStaysWithControls(controlsShownAtDown: Boolean, touchOnControls: Boolean): Boolean =
    controlsShownAtDown && touchOnControls
