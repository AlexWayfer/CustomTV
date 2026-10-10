package name.alexwayfer.customtv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * The lines a collapsible text lays out: every line while it is open or moving, so a closing text is
 * cut off as it shrinks rather than at once; one line once it has collapsed.
 */
internal fun collapsibleTextMaxLines(allLines: Boolean): Int = if (allLines) Int.MAX_VALUE else 1

/**
 * The height of a collapsible text at [progress] (0 collapsed, 1 open): its first line at least, and
 * the rest of its [fullPx] in step with the progress. Never below zero, nor past [fullPx].
 */
internal fun revealedLinesHeightPx(firstLinePx: Int, fullPx: Int, progress: Float): Int {
    val full = fullPx.coerceAtLeast(0)
    val first = firstLinePx.coerceIn(0, full)
    return (first + (full - first) * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(0, full)
}

/**
 * Shows the lines of a text laid out with [collapsibleTextMaxLines] from the first one down, as far as
 * [progress] says; [firstLineBottomPx] comes from the text's layout.
 */
internal fun Modifier.revealLines(firstLineBottomPx: () -> Int, progress: () -> Float): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = revealedLinesHeightPx(firstLineBottomPx(), placeable.height, progress())
        layout(placeable.width, height) { placeable.placeRelative(0, 0) }
    }

/** The spring the chat cards fold with: the one `expandVertically` and `animateContentSize` use by default. */
internal val CardFoldSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
