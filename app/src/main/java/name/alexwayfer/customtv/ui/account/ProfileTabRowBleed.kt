package name.alexwayfer.customtv.ui.account

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/**
 * Lays the element out [ProfileSideMargin] wider on each side than the profile's column allows, so the tab row reaches
 * the screen's edges, while the column still sees the width it gave.
 */
internal fun Modifier.bleedPastProfileSides(): Modifier = layout { measurable, constraints ->
    val bleedPx = ProfileSideMargin.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * bleedPx))
    val width = constraints.constrainWidth(bledElementReportedWidth(placeable.width, bleedPx))
    layout(width, placeable.height) {
        placeable.place(-bleedPx, 0)
    }
}

/** The width the parent sees: the element's own width without both bleeds, never below zero. */
internal fun bledElementReportedWidth(elementWidth: Int, bleedPx: Int): Int =
    (elementWidth - 2 * bleedPx).coerceAtLeast(0)
