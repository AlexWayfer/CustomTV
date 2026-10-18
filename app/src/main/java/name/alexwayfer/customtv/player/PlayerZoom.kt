package name.alexwayfer.customtv.player

import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO

/** The largest zoom of the player's picture. */
internal const val PLAYER_ZOOM_MAX = 4f

/** A pinch that leaves the zoom below this goes back to the whole picture, so a near miss of 1× does not stay. */
internal const val PLAYER_ZOOM_SNAP = 1.1f

/** A point on the player, in pixels from its center. */
internal data class ZoomPoint(val x: Float, val y: Float)

/** How the player's picture is zoomed: [scale] around the player's center, then moved [x] and [y] pixels. */
internal data class PlayerZoomTransform(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f) {
    val zoomed: Boolean get() = scale > 1f
}

/** The player's size, and the 16:9 video it shows in the middle, with black bars beside or above it. */
internal data class PlayerZoomFrame(val width: Float, val height: Float) {
    val videoWidth: Float get() = minOf(width, height * VIDEO_ASPECT_RATIO)
    val videoHeight: Float get() = minOf(height, width / VIDEO_ASPECT_RATIO)
}

/** Two fingers that started [startSpan] apart around [startFocus] and are now [span] apart around [focus]. */
internal data class PlayerPinch(val startSpan: Float, val span: Float, val startFocus: ZoomPoint, val focus: ZoomPoint)

/**
 * Where a point at [localX] and [localY] in the player's own, unzoomed coordinates shows under [transform]: what the
 * finger touches on the screen, measured from the player's center.
 */
internal fun playerZoomPoint(localX: Float, localY: Float, transform: PlayerZoomTransform, frame: PlayerZoomFrame) =
    ZoomPoint(
        x = transform.x + transform.scale * (localX - frame.width / 2),
        y = transform.y + transform.scale * (localY - frame.height / 2),
    )

/**
 * The zoom a pinch makes of [start]: scaled as far as the fingers spread, between the whole picture and
 * [PLAYER_ZOOM_MAX], with the picture's point that was under the fingers following them.
 */
internal fun pinchedPlayerZoom(
    start: PlayerZoomTransform,
    pinch: PlayerPinch,
    frame: PlayerZoomFrame,
): PlayerZoomTransform {
    val scale = (start.scale * pinch.span / pinch.startSpan).coerceIn(1f, PLAYER_ZOOM_MAX)
    val ratio = scale / start.scale
    return PlayerZoomTransform(
        scale = scale,
        x = pinch.focus.x - (pinch.startFocus.x - start.x) * ratio,
        y = pinch.focus.y - (pinch.startFocus.y - start.y) * ratio,
    ).within(frame)
}

/** The zoomed picture of [start] moved [dx] and [dy] pixels with the finger. */
internal fun pannedPlayerZoom(start: PlayerZoomTransform, dx: Float, dy: Float, frame: PlayerZoomFrame) =
    start.copy(x = start.x + dx, y = start.y + dy).within(frame)

/**
 * The zoom moved back as little as needed for the video to cover the player on each side, or to stay in the middle
 * where it is still narrower or lower than the player.
 */
internal fun PlayerZoomTransform.within(frame: PlayerZoomFrame): PlayerZoomTransform {
    val maxX = ((scale * frame.videoWidth - frame.width) / 2).coerceAtLeast(0f)
    val maxY = ((scale * frame.videoHeight - frame.height) / 2).coerceAtLeast(0f)
    return copy(x = x.coerceIn(-maxX, maxX), y = y.coerceIn(-maxY, maxY))
}

/** Where the zoom rests once the fingers lift: a pinch that barely zooms goes back to the whole picture. */
internal fun settledPlayerZoom(transform: PlayerZoomTransform): PlayerZoomTransform =
    if (transform.scale < PLAYER_ZOOM_SNAP) PlayerZoomTransform() else transform
