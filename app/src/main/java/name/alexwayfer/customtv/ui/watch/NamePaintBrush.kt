package name.alexwayfer.customtv.ui.watch

import android.content.Context
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import name.alexwayfer.customtv.chat.NamePaint
import name.alexwayfer.customtv.chat.PaintFunction
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * The style of a name painted with [paint] that lies at [box] in its text, or null while the box is not laid out yet or
 * the paint's picture is loading; the name keeps its color until then.
 */
@Composable
internal fun rememberNamePaintSpan(paint: NamePaint?, fallback: Color, box: Rect?): SpanStyle? {
    val image = rememberPaintImage(paint?.imageUrl)
    val density = LocalDensity.current
    return remember(paint, fallback, box, image, density) {
        if (paint == null || box == null) null else namePaintStyle(paint, image, fallback, box, density)
    }
}

/** The style of a name painted with [paint] that is the whole of its text, or null while its picture is loading. */
@Composable
internal fun rememberNamePaintTextStyle(paint: NamePaint?, fallback: Color): SpanStyle? {
    val image = rememberPaintImage(paint?.imageUrl)
    val density = LocalDensity.current
    return remember(paint, fallback, image, density) {
        paint?.let { namePaintStyle(it, image, fallback, box = null, density) }
    }
}

private fun namePaintStyle(
    paint: NamePaint,
    image: ImageBitmap?,
    fallback: Color,
    box: Rect?,
    density: Density,
): SpanStyle? {
    if (paint.function == PaintFunction.Image && image == null) return null
    val shadow = paint.shadow?.let {
        with(density) {
            Shadow(it.color, Offset(it.x.dp.toPx(), it.y.dp.toPx()), blurRadius = it.radius.dp.toPx())
        }
    }
    return SpanStyle(brush = NamePaintBrush(paint, image, paint.color ?: fallback, box), shadow = shadow)
}

/**
 * Draws a paint over [box] of a text, whatever size the text gives the brush: a span's brush gets the size of the whole
 * paragraph, while CSS paints only the name. Without a box it paints the size it gets. A paint that cannot be drawn,
 * such as a gradient of one color, shows [fallback].
 */
private class NamePaintBrush(
    private val paint: NamePaint,
    private val image: ImageBitmap?,
    private val fallback: Color,
    private val box: Rect?,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        namePaintShader(paint, box ?: Rect(Offset.Zero, size), image)
            ?: LinearGradientShader(Offset.Zero, Offset(1f, 0f), listOf(fallback, fallback))

    override fun equals(other: Any?): Boolean = other is NamePaintBrush && other.paint == paint &&
        other.image == image && other.fallback == fallback && other.box == box

    override fun hashCode(): Int = listOf(paint, image, fallback, box).hashCode()
}

private fun namePaintShader(paint: NamePaint, box: Rect, image: ImageBitmap?): Shader? {
    if (box.width <= 0f || box.height <= 0f) return null
    if (paint.function == PaintFunction.Image) return image?.let { imageShader(it, box) }
    // Android refuses a gradient of fewer than two colors.
    if (paint.stops.size < 2) return null
    val span = if (paint.repeat) repeatingSpan(paint.stops) else GradientSpan(0f, 1f, paint.stops)
    val gradient = GradientColors(
        colors = span.stops.map { it.color },
        positions = orderedStopPositions(span.stops.map { it.at }),
        tileMode = if (paint.repeat) TileMode.Repeated else TileMode.Clamp,
    )
    return when (paint.function) {
        PaintFunction.Linear -> {
            val (start, end) = linearGradientEnds(paint.angle, box)
            LinearGradientShader(
                lerp(start, end, span.from),
                lerp(start, end, span.to),
                gradient.colors,
                gradient.positions,
                gradient.tileMode,
            )
        }
        // A repeating one repeats from the center instead of its first stop, close enough for a name.
        PaintFunction.Radial -> radialShader(paint.ellipse, box, span.to, gradient)
        PaintFunction.Conic -> SweepGradientShader(box.center, gradient.colors, gradient.positions).also { shader ->
            shader.setLocalMatrix(Matrix().apply { setRotate(CONIC_START_DEGREES, box.center.x, box.center.y) })
        }
        PaintFunction.Image -> null
    }
}

private class GradientColors(val colors: List<Color>, val positions: List<Float>, val tileMode: TileMode)

// CSS starts a conic gradient at the top, Android at the right.
private const val CONIC_START_DEGREES = -90f

private fun radialShader(ellipse: Boolean, box: Rect, reach: Float, gradient: GradientColors): Shader {
    // CSS's ellipse reaches the farthest corner too, keeping the box's proportions.
    val radiusX = if (ellipse) box.width / 2f * sqrt(2f) else farthestCornerRadius(box)
    val radiusY = if (ellipse) box.height / 2f * sqrt(2f) else radiusX
    // Android refuses a radius that is not positive.
    val radius = (radiusX * reach).coerceAtLeast(1f)
    val shader = RadialGradientShader(box.center, radius, gradient.colors, gradient.positions, gradient.tileMode)
    if (ellipse) shader.setLocalMatrix(Matrix().apply { setScale(1f, radiusY / radiusX, box.center.x, box.center.y) })
    return shader
}

// CSS stretches a paint's picture over the whole name.
private fun imageShader(image: ImageBitmap, box: Rect): Shader? {
    if (image.width <= 0 || image.height <= 0) return null
    return ImageShader(image, TileMode.Clamp, TileMode.Clamp).also { shader ->
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(box.width / image.width, box.height / image.height)
                postTranslate(box.left, box.top)
            },
        )
    }
}

/** Paint pictures by URL; there are a few dozen paints, and every message of a painted chatter draws one. */
private val paintImages = ConcurrentHashMap<String, ImageBitmap>()

@Composable
private fun rememberPaintImage(url: String?): ImageBitmap? {
    val context = LocalContext.current
    // The state outlives a change of the URL, so each run sets the picture of its own.
    val image by produceState(url?.let(paintImages::get), url) {
        value = url?.let { paintImages[it] ?: loadPaintImage(context, it) }
    }
    return image
}

// A shader needs the pixels in memory; an animated picture gives its first frame.
private suspend fun loadPaintImage(context: Context, url: String): ImageBitmap? {
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
    val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
    if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) return null
    return drawable.toBitmap().asImageBitmap().also { paintImages[url] = it }
}
