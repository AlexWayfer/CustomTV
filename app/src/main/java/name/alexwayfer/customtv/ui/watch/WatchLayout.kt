package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

internal fun Modifier.fixedPlayerLayout(widthPx: Int, heightPx: Int): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
        layout(widthPx, heightPx) {
            placeable.placeRelative(0, 0)
        }
    }

/**
 * Keeps the 16:9 player at the full [fullWidthPx] and scales its picture down to the box, so a
 * collapse animation does not make the web page lay itself out again on every frame.
 */
internal fun Modifier.playerScaledFromFullWidth(fullWidthPx: Int): Modifier =
    layout { measurable, constraints ->
        val fullHeightPx = (fullWidthPx * 9f / 16f).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(fullWidthPx, fullHeightPx))
        val scale = playerPictureScale(constraints.maxWidth, fullWidthPx)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(0, 0) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
            }
        }
    }

/**
 * How far the full screen player keeps from each side: the wider side of the display cutout on both, so the
 * video stays centered with the cutout over a black bar, and the player's controls stay clear of it.
 */
internal fun fullscreenCutoutSidePx(leftPx: Int, rightPx: Int): Int = maxOf(leftPx, rightPx, 0)

/** How much the full-width player picture shrinks to fill a box [boxWidthPx] wide. */
internal fun playerPictureScale(boxWidthPx: Int, fullWidthPx: Int): Float =
    if (fullWidthPx <= 0) 1f else boxWidthPx.toFloat() / fullWidthPx

/**
 * How far the chat scrolls to keep its newest messages where they were when its visible height
 * changes: the list's height less the notices laid over its top.
 */
internal fun chatAnchorShift(oldHeight: Int, oldTopInset: Int, newHeight: Int, newTopInset: Int): Int =
    (oldHeight - oldTopInset) - (newHeight - newTopInset)

/**
 * Keeps the newest messages in place when the list resizes or the notices over it grow or shrink
 * ([topInsetPx]), unless the chat is scrolled to its start.
 */
internal fun Modifier.keepChatAnchoredOnResize(listState: LazyListState, topInsetPx: Int): Modifier =
    layout { measurable, constraints ->
        val oldHeight = listState.layoutInfo.viewportSize.height
        val shift = chatAnchorShift(
            oldHeight = oldHeight,
            oldTopInset = listState.layoutInfo.beforeContentPadding,
            newHeight = constraints.maxHeight,
            newTopInset = topInsetPx,
        )
        val atChatStart = listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset <= 0
        if (oldHeight > 0 && shift != 0 && !atChatStart) {
            listState.requestScrollToItem(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset + shift,
            )
        }
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

@Composable
internal fun Modifier.blockClicks(): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = {},
)
