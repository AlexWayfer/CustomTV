package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Expand vertically only; leave the neighbouring badges and message text outside the target. */
internal fun authorTouchBounds(bounds: Rect, extra: Float): Rect =
    Rect(bounds.left, bounds.top - extra, bounds.right, bounds.bottom + extra)

/**
 * Whether the text a layout was made for ([laidOut]) is [text], so its offsets match the link ranges. Text with
 * links is laid out with the link styles added as span styles, so the whole annotated strings never compare equal.
 */
internal fun layoutTextMatches(laidOut: AnnotatedString, text: AnnotatedString): Boolean =
    laidOut.text == text.text

@Composable
internal fun ChatAuthorTouchTarget(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    if (start >= end) return
    val density = LocalDensity.current
    val extra = with(density) { 4.dp.toPx() }
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(end - 1)
    for (line in firstLine..lastLine) {
        val from = maxOf(start, layout.getLineStart(line))
        val to = minOf(end, layout.getLineEnd(line, visibleEnd = true))
        if (from >= to) continue
        val bounds = authorTouchBounds(
            layout.getPathForRange(from, to).getBounds(), extra,
        )
        Box(
            Modifier
                .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                // The original text link remains the single accessible action.
                .clearAndSetSemantics {},
        )
    }
}
