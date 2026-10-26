package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color

/** How a 7TV paint fills a name: a CSS gradient of its kind, or a picture stretched over the name. */
internal enum class PaintFunction { Linear, Radial, Conic, Image }

/** A color of a paint's gradient at [at], from 0 at the start of its line to 1 at its end. */
internal data class PaintStop(val at: Float, val color: Color)

/** A shadow under a painted name, in CSS pixels, which the app draws as dp. */
internal data class PaintShadow(val x: Float, val y: Float, val radius: Float, val color: Color)

/**
 * A 7TV paint: a gradient or a picture that fills a chatter's name instead of their color. Only the paint's first layer
 * and its first shadow are kept, as Xtra draws them.
 */
internal data class NamePaint(
    val function: PaintFunction,
    val stops: List<PaintStop> = emptyList(),
    /** A linear gradient's direction in CSS degrees: 0 runs upwards, 90 to the right. */
    val angle: Float = 0f,
    val repeat: Boolean = false,
    /** A radial gradient stretched to the name's box instead of a circle. */
    val ellipse: Boolean = false,
    val imageUrl: String? = null,
    val shadow: PaintShadow? = null,
    /** The paint's own color, shown while it cannot be drawn; null keeps the chatter's. */
    val color: Color? = null,
)
