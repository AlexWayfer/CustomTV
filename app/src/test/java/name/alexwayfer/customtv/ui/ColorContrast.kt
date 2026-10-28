package name.alexwayfer.customtv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG AA for normal text, such as a swipe step's 14 sp name. */
internal const val MIN_TEXT_CONTRAST = 4.5f

private const val LUMINANCE_OFFSET = 0.05f

/** The WCAG contrast ratio of two colors. */
internal fun contrast(a: Color, b: Color): Float {
    val lighter = max(a.luminance(), b.luminance())
    val darker = min(a.luminance(), b.luminance())
    return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}
