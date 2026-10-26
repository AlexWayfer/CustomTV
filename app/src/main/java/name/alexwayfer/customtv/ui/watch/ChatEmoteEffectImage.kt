package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
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
    val animated = !LocalPlayerContentHidden.current && emoteEffectsMove(effects)
    // Read only while drawing and in the layer, so a tick redraws the emote without recomposing its row.
    val frameTime = rememberEmoteEffectTime(animated)
    val stillColorFilter = remember(effects) { emoteEffectColorFilter(effects, 0L) }
    SharedEmoteImage(
        url = url,
        contentDescription = contentDescription,
        size = size,
        aspectRatio = aspectRatio,
        colorFilter = if (emoteEffectColorMoves(effects)) {
            { emoteEffectColorFilter(effects, frameTime.value) }
        } else {
            { stillColorFilter }
        },
        modifier = Modifier.graphicsLayer { applyEmoteMotion(effects, frameTime.value) },
    )
}

/** Whether [effects] change over time, so the emote needs a new frame now and then. */
internal fun emoteEffectsMove(effects: EmoteEffects): Boolean =
    effects.party || effects.rainbow || effects.shake || effects.hyperShake || effects.jam || effects.bounce

/** The time emote effects draw at, ticking while [animated]. */
@Composable
internal fun rememberEmoteEffectTime(animated: Boolean): State<Long> =
    // Frame timestamps share one monotonic origin, even when rows enter the viewport later.
    produceState(initialValue = emoteEffectTime(System.nanoTime()), key1 = animated) {
        if (animated) {
            while (true) {
                val frame = withFrameNanos { it }
                value = emoteEffectTime(frame)
                delay(nanosUntilNextEmoteEffectFrame(frame).nanoseconds)
            }
        }
    }

internal fun emoteEffectColorFilter(effects: EmoteEffects, frameTimeNanos: Long): ColorFilter? = when {
    effects.cursed -> ColorFilter.colorMatrix(cursedColorMatrix())
    effects.hyperRed -> ColorFilter.tint(HyperRedTint, BlendMode.Modulate)
    effects.rainbow -> ColorFilter.tint(
        Color.hsv(effectPhase(frameTimeNanos, RAINBOW_CYCLE_MILLIS) * FULL_TURN_DEGREES, RAINBOW_SATURATION, 1f),
        BlendMode.Modulate,
    )
    effects.party -> ColorFilter.colorMatrix(partyColorMatrix(effectPhase(frameTimeNanos, PARTY_CYCLE_MILLIS)))
    else -> null
}

private val HyperRedTint = Color(0xFFFF2020)

/** The rainbow goes once around the hue circle in this time; the party colors take a little less. */
private const val RAINBOW_CYCLE_MILLIS = 2_000L
private const val PARTY_CYCLE_MILLIS = 1_500L
private const val FULL_TURN_DEGREES = 360f
private const val RAINBOW_SATURATION = 0.9f

private fun GraphicsLayerScope.applyEmoteMotion(effects: EmoteEffects, frameTimeNanos: Long) {
    val motion = emoteEffectMotion(effects, frameTimeNanos)
    scaleX = motion.scaleX
    scaleY = motion.scaleY
    rotationZ = motion.rotationDegrees
    translationX = motion.translationX
    translationY = motion.translationY
    transformOrigin = motion.origin
}

/** How an emote with effects is moved, turned, and scaled on one frame, around [origin]. */
internal class EmoteMotion(
    val scaleX: Float,
    val scaleY: Float,
    val rotationDegrees: Float,
    val translationX: Float,
    val translationY: Float,
    val origin: TransformOrigin,
)

internal fun emoteEffectMotion(effects: EmoteEffects, frameTimeNanos: Long): EmoteMotion {
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
    return EmoteMotion(
        scaleX = (if (effects.flipX) -1f else 1f) * effects.widthMultiplier * bounceScaleX,
        scaleY = (if (effects.flipY) -1f else 1f) * bounceScaleY,
        rotationDegrees = effects.rotationDegrees + if (effects.jam) jam.rotationDegrees else 0f,
        translationX = motion.x,
        translationY = motion.y,
        origin = if (effects.bounce) BottomCenterOrigin else TransformOrigin.Center,
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

/** A bouncing emote squashes against the bottom of its line. */
private val BottomCenterOrigin = TransformOrigin(TransformOrigin.Center.pivotFractionX, 1f)

// CSS grayscale(1) brightness(0.7) contrast(2.5): 1.75 * luma - 191.25.
private val CURSED_COLOR_VALUES = floatArrayOf(
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    .37205f, 1.2516f, .12635f, 0f, -191.25f,
    0f, 0f, 0f, 1f, 0f,
)

private fun cursedColorMatrix(): ColorMatrix = ColorMatrix(CURSED_COLOR_VALUES.copyOf())

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
    return rgbColorMatrix(multiplyColors(saturated, multiplyColors(hue, sepia)))
}

/** Red, green, and blue: the rows and columns of a color-only matrix. */
private const val RGB = 3

/** [rgb], a row-major RGB matrix, as a [ColorMatrix] that keeps alpha and adds no offset. */
private fun rgbColorMatrix(rgb: FloatArray): ColorMatrix {
    val matrix = ColorMatrix()
    for (row in 0 until RGB) {
        for (column in 0 until RGB) matrix[row, column] = rgb[row * RGB + column]
    }
    return matrix
}

private fun multiplyColors(left: FloatArray, right: FloatArray): FloatArray = FloatArray(RGB * RGB) { index ->
    val row = index / RGB
    val column = index % RGB
    left[row * RGB] * right[column] +
        left[row * RGB + 1] * right[RGB + column] +
        left[row * RGB + 2] * right[2 * RGB + column]
}
