package name.alexwayfer.customtv.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import kotlin.math.cos
import kotlin.math.sin

private const val GLOW_TURN_MILLIS = 6000
private val GLOW_STEP = 1.dp

/** The angle of [premiumGlowBrush]'s colors, turning slowly all the time. */
@Composable
internal fun rememberPremiumGlowAngle(): State<Float> =
    rememberInfiniteTransition(label = "premium border").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(GLOW_TURN_MILLIS, easing = LinearEasing)),
        label = "premium border angle",
    )

/** Corner radii in pixels, clockwise from the top left. */
internal data class GlowCorners(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {
    fun scaled(factor: Float) =
        GlowCorners(topLeft * factor, topRight * factor, bottomRight * factor, bottomLeft * factor)

    companion object {
        val Square = GlowCorners(0f, 0f, 0f, 0f)

        fun all(radius: Float) = GlowCorners(radius, radius, radius, radius)
    }
}

/**
 * How a premium border looks: a [line] at [lineAlpha], and a glow [glow] deep that starts at [glowAlpha] and fades
 * away from the line, out of the shape when [outward] or into it otherwise. An outward line sits on the edge; an
 * inward one keeps inside it.
 */
internal class PremiumGlowStyle(
    val line: Dp,
    val lineAlpha: Float,
    val glow: Dp,
    val glowAlpha: Float,
    val outward: Boolean,
)

/**
 * A premium border around a rounded rectangle with [corners], in [style], whose colors turn by [angle], read only
 * while drawing. The glow is drawn as thin rings, each a little fainter than the one before.
 */
internal fun Modifier.premiumGlow(corners: GlowCorners, style: PremiumGlowStyle, angle: () -> Float): Modifier =
    drawWithCache {
        val line = style.line.toPx()
        val step = GLOW_STEP.toPx()
        val layers = (style.glow / GLOW_STEP).toInt()
        val lineInset = if (style.outward) 0f else line / 2
        val direction = if (style.outward) -1f else 1f
        val linePath = Path().apply { addRoundRect(premiumGlowRing(size, corners, lineInset)) }
        val glowPaths = List(layers) { layer ->
            val inset = direction * (lineInset + line / 2 + step * (layer + 0.5f))
            Path().apply { addRoundRect(premiumGlowRing(size, corners, inset)) }
        }
        val lineStroke = Stroke(line)
        val glowStroke = Stroke(step)
        onDrawWithContent {
            drawContent()
            val brush = premiumGlowBrush(size, angle())
            glowPaths.forEachIndexed { layer, path ->
                drawPath(path, brush, alpha = style.glowAlpha * premiumGlowFade(layer, layers), style = glowStroke)
            }
            drawPath(linePath, brush, alpha = style.lineAlpha, style = lineStroke)
        }
    }

/**
 * A rectangle [inset] inside [size], or outside it when [inset] is negative, whose corners keep the centers of
 * [corners], so the rings stay parallel to the edge. It never turns inside out on a size smaller than twice the inset.
 */
internal fun premiumGlowRing(size: Size, corners: GlowCorners, inset: Float): RoundRect {
    fun radius(edge: Float) = (edge - inset).coerceAtLeast(0f).let { CornerRadius(it, it) }
    return RoundRect(
        left = inset,
        top = inset,
        right = (size.width - inset).coerceAtLeast(inset),
        bottom = (size.height - inset).coerceAtLeast(inset),
        topLeftCornerRadius = radius(corners.topLeft),
        topRightCornerRadius = radius(corners.topRight),
        bottomRightCornerRadius = radius(corners.bottomRight),
        bottomLeftCornerRadius = radius(corners.bottomLeft),
    )
}

/** How strong the glow ring [layer] of [layers] is: full at the line, fading to nothing away from it. */
internal fun premiumGlowFade(layer: Int, layers: Int): Float {
    if (layers <= 0) return 0f
    val left = (1f - layer.toFloat() / layers).coerceIn(0f, 1f)
    return left * left
}

/** The gold and purple colors of a premium border over [size], turned by [degrees]. */
internal fun premiumGlowBrush(size: Size, degrees: Float): Brush {
    val (start, end) = premiumGlowLine(size, degrees)
    return Brush.linearGradient(
        colors = listOf(PremiumGold, TwitchPurple.copy(alpha = 0.4f), PremiumGold),
        start = start,
        end = end,
    )
}

/**
 * The line the border's gradient runs along: through the center at [degrees], long enough to cover the corners at
 * every angle. On a zero size both ends are the origin.
 */
internal fun premiumGlowLine(size: Size, degrees: Float): Pair<Offset, Offset> {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = center.getDistance()
    val radians = Math.toRadians(degrees.toDouble())
    val reach = Offset(cos(radians).toFloat(), sin(radians).toFloat()) * radius
    return (center - reach) to (center + reach)
}
