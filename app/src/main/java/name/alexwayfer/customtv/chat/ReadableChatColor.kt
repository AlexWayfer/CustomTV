package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Twitch "Readable Colors": keep hue, adjust lightness until WCAG 2.1
 * contrast against the chat background is at least 6:1.
 */
object ReadableChatColor {
    const val MIN_CONTRAST = 6f

    /** Halvings of the lightness range; 24 leave a step far below one color level. */
    private const val SEARCH_STEPS = 24

    // WCAG 2.1 relative luminance and contrast ratio, over sRGB.
    private const val CONTRAST_FLARE = 0.05f
    private const val RED_WEIGHT = 0.2126f
    private const val GREEN_WEIGHT = 0.7152f
    private const val BLUE_WEIGHT = 0.0722f
    private const val SRGB_LINEAR_LIMIT = 0.04045f
    private const val SRGB_LINEAR_SLOPE = 12.92f
    private const val SRGB_OFFSET = 0.055f
    private const val SRGB_GAMMA = 2.4f

    private const val FULL_TURN_DEGREES = 360f

    fun adjust(foreground: Color, background: Color): Color {
        if (contrastRatio(foreground, background) >= MIN_CONTRAST) {
            return foreground
        }
        val hsl = rgbToHsl(foreground)
        val lighten = relativeLuminance(background) < 0.5f
        var low = if (lighten) hsl.l else 0f
        var high = if (lighten) 1f else hsl.l
        var best = if (lighten) Color.White else Color.Black
        repeat(SEARCH_STEPS) {
            val mid = (low + high) / 2f
            val candidate = hslToColor(hsl.h, hsl.s, mid)
            if (contrastRatio(candidate, background) >= MIN_CONTRAST) {
                best = candidate
                if (lighten) high = mid else low = mid
            } else {
                if (lighten) low = mid else high = mid
            }
        }
        return best
    }

    fun contrastRatio(first: Color, second: Color): Float {
        val lighter = max(relativeLuminance(first), relativeLuminance(second))
        val darker = min(relativeLuminance(first), relativeLuminance(second))
        return (lighter + CONTRAST_FLARE) / (darker + CONTRAST_FLARE)
    }

    private fun relativeLuminance(color: Color): Float {
        return RED_WEIGHT * linearize(color.red) +
            GREEN_WEIGHT * linearize(color.green) +
            BLUE_WEIGHT * linearize(color.blue)
    }

    private fun linearize(channel: Float): Float {
        return if (channel <= SRGB_LINEAR_LIMIT) {
            channel / SRGB_LINEAR_SLOPE
        } else {
            ((channel + SRGB_OFFSET) / (1f + SRGB_OFFSET)).pow(SRGB_GAMMA)
        }
    }

    private data class Hsl(val h: Float, val s: Float, val l: Float)

    private fun rgbToHsl(color: Color): Hsl {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val lightness = (max + min) / 2f
        if (max == min) {
            return Hsl(0f, 0f, lightness)
        }
        val delta = max - min
        val saturation = if (lightness > 0.5f) {
            delta / (2f - max - min)
        } else {
            delta / (max + min)
        }
        val hue = when (max) {
            r -> ((g - b) / delta + if (g < b) 6f else 0f) / 6f
            g -> ((b - r) / delta + 2f) / 6f
            else -> ((r - g) / delta + 4f) / 6f
        }
        return Hsl(hue * FULL_TURN_DEGREES, saturation, lightness)
    }

    private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
        return Color.hsl(
            hue = hue.mod(FULL_TURN_DEGREES),
            saturation = saturation.coerceIn(0f, 1f),
            lightness = lightness.coerceIn(0f, 1f),
        )
    }
}
