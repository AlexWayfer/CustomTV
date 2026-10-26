package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.player.PlayerPlayback
import name.alexwayfer.customtv.ui.components.VIDEO_ASPECT_RATIO
import kotlin.math.roundToInt

/** How far a drag on the stream info has hidden the stream, from 0 shown to 1 gone, over [travelPx]. */
internal fun streamHideDragProgress(startHidden: Boolean, offsetY: Float, travelPx: Float): Float {
    val share = offsetY / travelPx.coerceAtLeast(1f)
    return if (startHidden) (1f - share).coerceIn(0f, 1f) else (-share).coerceIn(0f, 1f)
}

/**
 * Whether the stream ends hidden when a drag on the stream info is let go: it switches past the minimize gesture's
 * share of the way, or when flung that way as fast as the minimize gesture takes.
 */
internal fun streamHiddenAfterDrag(startHidden: Boolean, progress: Float, velocityY: Float): Boolean =
    if (startHidden) {
        1f - progress <= MINIMIZE_DRAG_PROGRESS && velocityY <= MINIMIZE_FLING_VELOCITY
    } else {
        progress > MINIMIZE_DRAG_PROGRESS || velocityY < -MINIMIZE_FLING_VELOCITY
    }

/** Chat-only lasts while the stream stays paused. */
internal fun streamStaysHidden(hidden: Boolean, playback: PlayerPlayback): Boolean =
    hidden && playback == PlayerPlayback.Paused

/** The height left of [heightPx] with [hiddenFraction] of it closed, never below zero. */
internal fun streamRoomHeightPx(heightPx: Int, hiddenFraction: Float): Int =
    (heightPx * (1f - hiddenFraction.coerceIn(0f, 1f))).roundToInt().coerceAtLeast(0)

/**
 * How far the video is out of sight: as hidden as the stream on the full player, and back in sight as it goes to the
 * corner ([collapse] from 0 full to 1 mini), so the mini player shows the paused stream.
 */
internal fun hiddenVideoFraction(hidden: Float, collapse: Float): Float =
    (hidden * (1f - collapse.coerceIn(0f, 1f))).coerceIn(0f, 1f)

/**
 * How much of the player's room over the stream info is closed. In landscape ([overScreen]) the stream comes over the
 * chat instead, so the room stays closed until the stream is all in sight and full screen takes over.
 */
internal fun streamRoomClosedFraction(hidden: Float, overScreen: Boolean): Float =
    if (overScreen && hidden > 0f) 1f else hidden

/**
 * Whether the full player's video covers the whole screen: in landscape while some of the stream is hidden, as it
 * leaves full screen or comes back to it. On its way to the corner ([collapse]) it keeps the player's own place.
 */
internal fun hiddenStreamCoversScreen(overScreen: Boolean, partlyHidden: Boolean, collapse: Float): Boolean =
    overScreen && partlyHidden && collapse == 0f

/**
 * How far down the stream info and the chat go in landscape ([overScreen]), [heightPx] tall: right under the bottom
 * of the video that covers the screen but the full screen stream info's room ([roomPx]), as far as the stream is in
 * sight. In portrait the room over them moves them.
 */
internal fun hiddenStreamContentShiftPx(heightPx: Float, hidden: Float, overScreen: Boolean, roomPx: Int): Float =
    if (overScreen && hidden > 0f) {
        (heightPx - roomPx).coerceAtLeast(0f) * (1f - hidden.coerceIn(0f, 1f))
    } else {
        0f
    }

/** The height of the video over the content of [heightPx], leaving the full screen stream info's [roomPx] under it. */
internal fun streamCoverHeightPx(heightPx: Int, roomPx: Int): Int = (heightPx - roomPx).coerceAtLeast(0)

/** How far a drag on the stream info travels: the full width player's height, or the screen's in landscape. */
internal fun streamHideTravelPx(widthPx: Int, heightPx: Int, overScreen: Boolean): Float =
    if (overScreen) heightPx.toFloat() else widthPx / VIDEO_ASPECT_RATIO

/** How small the video gets on its way out of sight. */
internal const val HIDDEN_VIDEO_SCALE = 0.9f
