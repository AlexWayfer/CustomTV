package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/** How wide a row's content fades out at an edge it scrolls past. */
private val ScrollFadeWidth = 16.dp

/**
 * Takes what a drag or a fling along a horizontal row leaves at its ends, so it never swipes a pager around the row,
 * such as Explore's tabs.
 */
internal object KeepHorizontalDrag : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        available.copy(y = 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available.copy(y = 0f)
}

/**
 * Fades the row's content out at each edge that more content lies beyond, so the cut there reads as more to scroll,
 * not as a broken layout. Put it before the row's `horizontalScroll(state)`.
 */
internal fun Modifier.horizontalScrollFade(state: ScrollState): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadePx = ScrollFadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, endX = fadePx),
                size = size.copy(width = fadePx),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black,
                    1f to Color.Transparent,
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = size.copy(width = fadePx),
                blendMode = BlendMode.DstIn,
            )
        }
    }
