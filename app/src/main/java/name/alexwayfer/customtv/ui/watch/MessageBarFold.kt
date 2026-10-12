package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/** How tall the message bar is while it folds away under an overlaid chat; never below zero. */
internal fun foldedHeightPx(heightPx: Int, shown: Float): Int =
    (heightPx.coerceAtLeast(0) * shown.coerceIn(0f, 1f)).roundToInt()

/**
 * Folds the message bar away from its top while [shown] goes from 1 to 0, clipped only while folded, so the
 * suggestions above an open bar still show.
 */
internal fun Modifier.foldedAway(shown: () -> Float): Modifier =
    graphicsLayer {
        val fraction = shown()
        clip = fraction < 1f
        alpha = fraction
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = foldedHeightPx(placeable.height, shown())
        layout(placeable.width, height) {
            placeable.place(0, height - placeable.height)
        }
    }
