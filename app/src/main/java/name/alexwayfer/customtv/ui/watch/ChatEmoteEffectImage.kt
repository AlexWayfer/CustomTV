package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.chat.EmoteEffects
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.nanoseconds

@Composable
internal fun ChatEmoteEffectImage(
    url: String,
    contentDescription: String?,
    size: Dp,
    aspectRatio: Float,
    effects: EmoteEffects,
) {
    // Most emotes have no effect; they skip the effect clock and layer, which every row entering view would compose.
    if (!effects.changesLook) {
        SharedEmoteImage(url = url, contentDescription = contentDescription, size = size, aspectRatio = aspectRatio)
        return
    }
    val animated = !LocalPlayerContentHidden.current && (
        effects.party || effects.rainbow || effects.shake ||
            effects.hyperShake || effects.jam || effects.bounce
        )
    // Frame timestamps share one monotonic origin, even when rows enter the viewport later.
    val frameTimeNanos by produceState(initialValue = emoteEffectTime(System.nanoTime()), key1 = animated) {
        if (animated) {
            while (true) {
                val frame = withFrameNanos { it }
                value = emoteEffectTime(frame)
                delay(nanosUntilNextEmoteEffectFrame(frame).nanoseconds)
            }
        }
    }
    // Only the color effects read the clock during composition; motion reads it in the layer below.
    val colorFilter = when {
        effects.cursed -> ColorFilter.colorMatrix(cursedColorMatrix())
        effects.hyperRed -> ColorFilter.tint(Color(0xFFFF2020), BlendMode.Modulate)
        effects.rainbow -> ColorFilter.tint(
            Color.hsv(effectPhase(frameTimeNanos, 2_000) * 360f, 0.9f, 1f),
            BlendMode.Modulate,
        )
        effects.party -> ColorFilter.colorMatrix(partyColorMatrix(effectPhase(frameTimeNanos, 1_500)))
        else -> null
    }
    SharedEmoteImage(
        url = url,
        contentDescription = contentDescription,
        size = size,
        aspectRatio = aspectRatio,
        colorFilter = colorFilter,
        modifier = Modifier.graphicsLayer {
            val hyperShake = FFZ_HYPER_SHAKE.frameAt(effectPhase(frameTimeNanos, 100))
            val bttvShake = BTTV_SHAKE.frameAt(effectPhase(frameTimeNanos, 500), stepped = true)
            val jam = FFZ_JAM.frameAt(effectPhase(frameTimeNanos, 600))
            val bounce = FFZ_BOUNCE.frameAt(effectPhase(frameTimeNanos, 500))
            val motion = when {
                effects.hyperShake -> hyperShake
                effects.shake -> bttvShake
                effects.jam -> jam
                else -> MotionFrame()
            }
            val bounceScaleX = if (effects.bounce) bounce.scaleX else 1f
            val bounceScaleY = if (effects.bounce) bounce.scaleY else 1f
            scaleX = (if (effects.flipX) -1f else 1f) * effects.widthMultiplier * bounceScaleX
            scaleY = (if (effects.flipY) -1f else 1f) * bounceScaleY
            rotationZ = effects.rotationDegrees + if (effects.jam) jam.rotationDegrees else 0f
            translationX = motion.x
            translationY = motion.y
            transformOrigin = if (effects.bounce) androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) else androidx.compose.ui.graphics.TransformOrigin.Center
        },
    )
}

private fun effectPhase(frameTimeNanos: Long, durationMillis: Long): Float {
    val durationNanos = durationMillis * 1_000_000L
    return (frameTimeNanos % durationNanos).toFloat() / durationNanos
}

private data class MotionFrame(
    val x: Float = 0f,
    val y: Float = 0f,
    val rotationDegrees: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
)

private data class MotionFrames(val frames: List<MotionFrame>) {
    fun frameAt(phase: Float, stepped: Boolean = false): MotionFrame {
        val scaled = phase.coerceIn(0f, 0.9999f) * (frames.size - 1)
        val index = scaled.toInt()
        if (stepped || index == frames.lastIndex) return frames[index]
        val fraction = scaled - index
        val start = frames[index]
        val end = frames[index + 1]
        return MotionFrame(
            x = start.x + (end.x - start.x) * fraction,
            y = start.y + (end.y - start.y) * fraction,
            rotationDegrees = start.rotationDegrees + (end.rotationDegrees - start.rotationDegrees) * fraction,
            scaleX = start.scaleX + (end.scaleX - start.scaleX) * fraction,
            scaleY = start.scaleY + (end.scaleY - start.scaleY) * fraction,
        )
    }
}

// Exact keyframe coordinates from BTTV's chat stylesheet.
private val BTTV_SHAKE = MotionFrames(listOf(
    MotionFrame(0f, 1f), MotionFrame(2f, 0f), MotionFrame(1f, -2f),
    MotionFrame(-2f, 1f), MotionFrame(0f, -1f), MotionFrame(2f, 2f),
    MotionFrame(-1f, -1f), MotionFrame(-2f, 2f), MotionFrame(2f, 1f),
    MotionFrame(-1f, -2f), MotionFrame(1f, 0f),
))

// Exact keyframe coordinates from FFZ's Hyper Shake animation.
private val FFZ_HYPER_SHAKE = MotionFrames(listOf(
    MotionFrame(1f, 1f), MotionFrame(-1f, -2f), MotionFrame(-3f, 0f),
    MotionFrame(3f, 2f), MotionFrame(1f, -1f), MotionFrame(-1f, 2f),
    MotionFrame(-3f, 1f), MotionFrame(3f, 1f), MotionFrame(-1f, -1f),
    MotionFrame(1f, 2f), MotionFrame(1f, -2f),
))

private val FFZ_JAM = MotionFrames(listOf(
    MotionFrame(-2f, -2f, -6f), MotionFrame(-1.5f, -2f, -8f), MotionFrame(1f, -1.5f, -8f),
    MotionFrame(3f, 2.5f, -6f), MotionFrame(3f, 4f, -2f), MotionFrame(2f, 4f, 3f),
    MotionFrame(1f, 4f, 3f), MotionFrame(-.5f, 3f, 2f), MotionFrame(-1.25f, 1f),
    MotionFrame(-1.75f, -.5f, -2f), MotionFrame(-2f, -2f, -5f),
))

private val FFZ_BOUNCE = MotionFrames(listOf(
    MotionFrame(scaleX = .8f, scaleY = 1f), MotionFrame(scaleX = .9f, scaleY = .8f),
    MotionFrame(scaleX = 1f, scaleY = .4f), MotionFrame(scaleX = 1.2f, scaleY = .3f),
    MotionFrame(scaleX = -1.2f, scaleY = .3f), MotionFrame(scaleX = -1f, scaleY = .4f),
    MotionFrame(scaleX = -.9f, scaleY = .8f), MotionFrame(scaleX = -.8f, scaleY = 1f),
    MotionFrame(scaleX = -.9f, scaleY = .8f), MotionFrame(scaleX = -1f, scaleY = .4f),
    MotionFrame(scaleX = -1.2f, scaleY = .3f), MotionFrame(scaleX = 1.2f, scaleY = .3f),
    MotionFrame(scaleX = 1f, scaleY = .4f), MotionFrame(scaleX = .9f, scaleY = .8f),
    MotionFrame(scaleX = .8f, scaleY = 1f),
))

// CSS grayscale(1) brightness(0.7) contrast(2.5): 1.75 * luma - 191.25.
private fun cursedColorMatrix(): ColorMatrix = ColorMatrix(floatArrayOf(
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    0f, 0f, 0f, 1f, 0f,
))

// CSS filter order in BTTV: sepia(0.5) hue-rotate(0..360deg) saturate(2.5).
private fun partyColorMatrix(phase: Float): ColorMatrix {
    val angle = phase * 2f * Math.PI.toFloat()
    val cosine = cos(angle)
    val sine = sin(angle)
    val sepia = floatArrayOf(
        .6965f, .3845f, .0945f,
        .1745f, .843f, .084f,
        .136f, .267f, .5655f,
    )
    val hue = floatArrayOf(
        .213f + .787f * cosine - .213f * sine,
        .715f - .715f * cosine - .715f * sine,
        .072f - .072f * cosine + .928f * sine,
        .213f - .213f * cosine + .143f * sine,
        .715f + .285f * cosine + .140f * sine,
        .072f - .072f * cosine - .283f * sine,
        .213f - .213f * cosine - .787f * sine,
        .715f - .715f * cosine + .715f * sine,
        .072f + .928f * cosine + .072f * sine,
    )
    val saturation = 2.5f
    val saturated = floatArrayOf(
        .213f + .787f * saturation, .715f - .715f * saturation, .072f - .072f * saturation,
        .213f - .213f * saturation, .715f + .285f * saturation, .072f - .072f * saturation,
        .213f - .213f * saturation, .715f - .715f * saturation, .072f + .928f * saturation,
    )
    val colors = multiplyColors(saturated, multiplyColors(hue, sepia))
    return ColorMatrix(floatArrayOf(
        colors[0], colors[1], colors[2], 0f, 0f,
        colors[3], colors[4], colors[5], 0f, 0f,
        colors[6], colors[7], colors[8], 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
}

private fun multiplyColors(left: FloatArray, right: FloatArray): FloatArray = FloatArray(9) { index ->
    val row = index / 3
    val column = index % 3
    left[row * 3] * right[column] +
        left[row * 3 + 1] * right[3 + column] +
        left[row * 3 + 2] * right[6 + column]
}
