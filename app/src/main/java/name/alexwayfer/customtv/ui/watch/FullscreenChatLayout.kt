package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide
import kotlin.math.roundToInt

/** Where the player and the chat sit across the full screen area, in pixels from its left edge. */
internal data class FullscreenChatBounds(
    val playerX: Int,
    val playerWidth: Int,
    val chatX: Int,
    val chatWidth: Int,
    /** The chat lies over the video on a see-through background instead of beside it. */
    val overPlayer: Boolean,
    val chatOnLeft: Boolean,
)

/** The chat takes [FULLSCREEN_CHAT_WIDTH] of the area on its side; null when the full screen chat is off. */
internal fun fullscreenChatBounds(
    mode: FullscreenChatMode,
    side: FullscreenChatSide,
    areaWidthPx: Int,
): FullscreenChatBounds? {
    val area = areaWidthPx.coerceAtLeast(0)
    val chatWidth = (area * FULLSCREEN_CHAT_WIDTH).roundToInt()
    val chatOnLeft = side == FullscreenChatSide.Left
    val chatX = if (chatOnLeft) 0 else area - chatWidth
    return when (mode) {
        FullscreenChatMode.Hidden -> null
        FullscreenChatMode.Overlay -> FullscreenChatBounds(
            playerX = 0,
            playerWidth = area,
            chatX = chatX,
            chatWidth = chatWidth,
            overPlayer = true,
            chatOnLeft = chatOnLeft,
        )
        FullscreenChatMode.Column -> FullscreenChatBounds(
            playerX = if (chatOnLeft) chatWidth else 0,
            playerWidth = area - chatWidth,
            chatX = chatX,
            chatWidth = chatWidth,
            overPlayer = false,
            chatOnLeft = chatOnLeft,
        )
    }
}

/**
 * Where the video and the chat sit at one moment of the keyboard opening or closing. The player box is the video's
 * own 16:9 rectangle at full size, placed at [videoX], [videoY] and scaled by [videoScale] from its top-left corner,
 * so the page lays itself out once and the keyboard only scales its picture.
 */
internal data class FullscreenPlacement(
    val videoX: Int,
    val videoY: Int,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoScale: Float,
    val chatX: Int,
    val chatWidth: Int,
    /** Room under the chat: the player's control bar while it shows, and the overlaid chat's edge gap. */
    val chatBottomInset: Int,
    /** Room above a chat laid over the video. */
    val chatTopInset: Int = 0,
)

/** The keyboard's height, and how far it has come up, from 0 to 1. */
internal data class FullscreenKeyboard(val heightPx: Int = 0, val progress: Float = 0f)

/**
 * What an overlaid chat keeps clear of: the player's control bar, [controlBarShown] from 0 to 1, and [chatEdgePx]
 * from the area's top and bottom.
 */
internal data class FullscreenChatInsets(
    val controlBarPx: Int,
    val controlBarShown: Float = 1f,
    val chatEdgePx: Int = 0,
)

/**
 * At rest the video is centered in the player's part of the area, and an overlaid chat takes the full height but
 * its edge gap above and below, rising above the control bar as it shows. With the [keyboard] up the video shrinks
 * into the top corner away from the chat, to the room above the keyboard and no wider than leaves the chat its usual
 * width; the chat takes the rest of the width. Between the two, the keyboard's progress moves everything along with
 * it.
 */
internal fun fullscreenPlacement(
    bounds: FullscreenChatBounds?,
    areaWidthPx: Int,
    areaHeightPx: Int,
    keyboard: FullscreenKeyboard,
    insets: FullscreenChatInsets,
): FullscreenPlacement {
    val area = areaWidthPx.coerceAtLeast(0)
    val height = areaHeightPx.coerceAtLeast(0)
    val playerX = bounds?.playerX ?: 0
    val playerWidth = (bounds?.playerWidth ?: area).coerceAtLeast(0)
    val videoWidth = minOf(playerWidth, (height * 16f / 9f).roundToInt())
    val videoHeight = (videoWidth * 9f / 16f).roundToInt()
    val restX = playerX + (playerWidth - videoWidth) / 2
    val restY = (height - videoHeight) / 2
    val rest = FullscreenPlacement(
        videoX = restX,
        videoY = restY,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        videoScale = 1f,
        chatX = bounds?.chatX ?: 0,
        chatWidth = bounds?.chatWidth ?: 0,
        chatBottomInset = if (bounds?.overPlayer == true) {
            val shown = insets.controlBarShown.coerceIn(0f, 1f)
            val edge = insets.chatEdgePx.coerceAtLeast(0)
            (edge + (insets.controlBarPx.coerceAtLeast(0) - edge) * shown).roundToInt()
        } else {
            0
        },
        chatTopInset = if (bounds?.overPlayer == true) insets.chatEdgePx.coerceAtLeast(0) else 0,
    )
    val progress = keyboard.progress.coerceIn(0f, 1f)
    val noVideo = videoWidth == 0 || videoHeight == 0
    if (bounds == null || progress == 0f || noVideo) return rest
    val room = (height - keyboard.heightPx).coerceAtLeast(0)
    val widthForVideo = (area - bounds.chatWidth).coerceAtLeast(0)
    val keyboardScale = minOf(1f, room.toFloat() / videoHeight, widthForVideo.toFloat() / videoWidth)
    val shownWidth = (videoWidth * keyboardScale).roundToInt()
    fun lerp(from: Int, to: Int): Int = (from + (to - from) * progress).roundToInt()
    return FullscreenPlacement(
        videoX = lerp(restX, if (bounds.chatOnLeft) area - shownWidth else 0),
        videoY = lerp(restY, 0),
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        videoScale = 1f + (keyboardScale - 1f) * progress,
        chatX = lerp(bounds.chatX, if (bounds.chatOnLeft) 0 else shownWidth),
        chatWidth = lerp(bounds.chatWidth, area - shownWidth),
        chatBottomInset = lerp(rest.chatBottomInset, 0),
        chatTopInset = lerp(rest.chatTopInset, 0),
    )
}

/**
 * How far the player's own buttons keep from the video's right edge: clear of a chat laid over the right side of
 * the video, so the chat does not cover them.
 */
internal fun fullscreenPlayerButtonsEndPx(bounds: FullscreenChatBounds?, rest: FullscreenPlacement): Int =
    if (bounds != null && bounds.overPlayer && !bounds.chatOnLeft) {
        (rest.videoX + rest.videoWidth - rest.chatX).coerceAtLeast(0)
    } else {
        0
    }

/** Lays the player box out as the video's rectangle, placed and scaled as [placement] says. */
internal fun Modifier.fullscreenVideo(placement: MeasureScope.(Constraints) -> FullscreenPlacement): Modifier =
    layout { measurable, constraints ->
        val shown = placement(constraints)
        val placeable = measurable.measure(Constraints.fixed(shown.videoWidth, shown.videoHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(shown.videoX, shown.videoY) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = shown.videoScale
                scaleY = shown.videoScale
            }
        }
    }

/**
 * Lays the chat out in its column, as [placement] says. A [compact] chat takes only the height it needs, at the
 * bottom of the column, so the video around it still takes taps; [compactHeight] keeps that height. A full chat shows
 * from the bottom up to [expandProgress] of the way from that height to its own, so it rises out of the compact one
 * and sinks back into it; read when placed.
 */
internal fun Modifier.fullscreenChat(
    compact: Boolean,
    compactHeight: FullscreenCompactChatHeight,
    expandProgress: () -> Float,
    placement: MeasureScope.(Constraints) -> FullscreenPlacement,
): Modifier =
    layout { measurable, constraints ->
        val shown = placement(constraints)
        val width = shown.chatWidth.coerceIn(0, constraints.maxWidth)
        val top = shown.chatTopInset.coerceIn(0, constraints.maxHeight)
        val height = (constraints.maxHeight - shown.chatBottomInset - top).coerceAtLeast(0)
        if (compact) {
            val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = height))
            compactHeight.px = placeable.height
            return@layout layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(shown.chatX, top + height - placeable.height)
            }
        }
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(shown.chatX, top) {
                val compactPx = compactHeight.sinkTargetPx?.invoke() ?: compactHeight.px
                val revealed = fullscreenChatRevealedHeightPx(compactPx, height, expandProgress())
                if (revealed < height) {
                    clip = true
                    shape = BottomPartShape(revealed.toFloat())
                }
            }
        }
    }

/** The height a compact chat took when it last showed, so the full chat can rise out of it and sink back into it. */
internal class FullscreenCompactChatHeight {
    var px = 0

    /** While the full chat sinks, the height of its rows the compact chat will show; null when none is on screen. */
    var sinkTargetPx: (() -> Int?)? = null
}

/**
 * How much of a full chat of [fullPx] shows from the bottom up, [progress] of the way from the compact chat's
 * [compactPx]; never below zero nor above the full height.
 */
internal fun fullscreenChatRevealedHeightPx(compactPx: Int, fullPx: Int, progress: Float): Int {
    val full = fullPx.coerceAtLeast(0)
    val from = compactPx.coerceIn(0, full)
    return (from + (full - from) * progress.coerceIn(0f, 1f)).roundToInt()
}

/** The bottom [heightPx] of the shape's area. */
private class BottomPartShape(private val heightPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, (size.height - heightPx).coerceAtLeast(0f), size.width, size.height))
}

/**
 * A chat laid over the video keeps clear of the screen's top and bottom, with every corner rounded; a column chat
 * stays square.
 */
internal fun fullscreenChatShape(bounds: FullscreenChatBounds): Shape =
    if (bounds.overPlayer) RoundedCornerShape(FullscreenChatCorner) else RectangleShape

private val FullscreenChatCorner = 12.dp

/** The share of the full screen width the chat takes. */
internal const val FULLSCREEN_CHAT_WIDTH = 0.35f

/** How opaque the overlaid chat's background is. */
internal const val FULLSCREEN_OVERLAY_CHAT_ALPHA = 0.5f
