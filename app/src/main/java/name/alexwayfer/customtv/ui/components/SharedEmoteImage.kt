package name.alexwayfer.customtv.ui.components

import android.graphics.drawable.Drawable
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlin.math.min
import kotlin.math.roundToInt

fun emoteAspectRatio(url: String): Float? = SharedEmoteStore.aspectRatio(url)

/** Keeps a broken fallback from collapsing an emote to no width. */
private const val MIN_EMOTE_ASPECT_RATIO = 0.01f

fun resolvedEmoteAspectRatio(url: String, fallback: Float): Float {
    return emoteAspectRatio(url) ?: fallback.coerceAtLeast(MIN_EMOTE_ASPECT_RATIO)
}

internal fun aspectRatioFromIntrinsic(width: Int, height: Int): Float? {
    if (width <= 0 || height <= 0) return null
    return width.toFloat() / height.toFloat()
}

@Composable
fun SharedEmoteImage(
    url: String,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    val context = LocalContext.current
    val holder = remember(url) { SharedEmoteStore.acquire(url) }
    DisposableEffect(url) {
        onDispose { SharedEmoteStore.release(url) }
    }
    val drawable = holder.drawable
    // A row scrolled into view mostly shows emotes already decoded; it starts no load for them.
    if (drawable == null) {
        LaunchedEffect(holder) {
            holder.load(context)
        }
    }
    val sourceAspect = resolvedEmoteAspectRatio(url, aspectRatio)
    Canvas(
        modifier = modifier
            .size(width = size * sourceAspect, height = size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        // Reading the animation tick here redraws a new frame without recomposing the emote.
        if (drawable == null || holder.tick < 0) return@Canvas
        drawDrawableKeepAspect(drawable, colorFilter)
    }
}

/** Draws [drawable] centered in [area] from the scope's origin, scaled to fit without changing its shape. */
internal fun DrawScope.drawDrawableKeepAspect(drawable: Drawable, colorFilter: ColorFilter?, area: Size = size) {
    val canvasWidth = area.width
    val canvasHeight = area.height
    val intrinsicWidth = drawable.intrinsicWidth
    val intrinsicHeight = drawable.intrinsicHeight
    if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        drawable.setBounds(0, 0, canvasWidth.roundToInt(), canvasHeight.roundToInt())
    } else {
        val scale = min(canvasWidth / intrinsicWidth, canvasHeight / intrinsicHeight)
        val drawWidth = (intrinsicWidth * scale).roundToInt().coerceAtLeast(1)
        val drawHeight = (intrinsicHeight * scale).roundToInt().coerceAtLeast(1)
        val left = ((canvasWidth - drawWidth) / 2f).roundToInt()
        val top = ((canvasHeight - drawHeight) / 2f).roundToInt()
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
    }
    drawIntoCanvas { canvas ->
        if (colorFilter != null) {
            canvas.nativeCanvas.saveLayer(
                0f,
                0f,
                canvasWidth,
                canvasHeight,
                Paint().apply { this.colorFilter = colorFilter.asAndroidColorFilter() },
            )
        }
        drawable.draw(canvas.nativeCanvas)
        if (colorFilter != null) canvas.nativeCanvas.restore()
    }
}
