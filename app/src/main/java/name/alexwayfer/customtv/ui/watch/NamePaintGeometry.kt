package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import name.alexwayfer.customtv.chat.PaintStop
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The ends of a CSS linear gradient at [angleDegrees] across [box]: 0° runs upwards, 90° to the right, and the line is
 * as long as the box seen along it, so the first and last colors land on its corners.
 */
internal fun linearGradientEnds(angleDegrees: Float, box: Rect): Pair<Offset, Offset> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
    val half = (abs(box.width * direction.x) + abs(box.height * direction.y)) / 2f
    return box.center - direction * half to box.center + direction * half
}

/** Where a repeating gradient's one period lies on its line, from 0 to 1, and its stops within that period. */
internal data class GradientSpan(val from: Float, val to: Float, val stops: List<PaintStop>)

/**
 * A CSS repeating gradient repeats from its first stop to its last; Android repeats a whole gradient, so the stops are
 * spread over that span. Stops all at one place leave the whole line, as there is no span to repeat.
 */
internal fun repeatingSpan(stops: List<PaintStop>): GradientSpan {
    val first = stops.firstOrNull()?.at ?: 0f
    val last = stops.lastOrNull()?.at ?: 1f
    if (last <= first) return GradientSpan(0f, 1f, stops)
    return GradientSpan(first, last, stops.map { it.copy(at = (it.at - first) / (last - first)) })
}

/** Stop positions Android accepts: within the line, and never behind an earlier one, as CSS reads them. */
internal fun orderedStopPositions(positions: List<Float>): List<Float> {
    var previous = 0f
    return positions.map { position ->
        position.coerceIn(previous, 1f).also { previous = it }
    }
}

/** A CSS radial gradient's circle reaches the box's farthest corner from its center. */
internal fun farthestCornerRadius(box: Rect): Float = hypot(box.width / 2f, box.height / 2f)
