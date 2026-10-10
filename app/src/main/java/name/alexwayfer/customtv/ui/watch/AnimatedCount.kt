package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToLong

/**
 * A count that runs to each new value, such as the viewers of a stream or the channel points on a
 * prediction outcome. The first value shows at once; a change mid-run continues from the number
 * on screen. It counts in [Long], so a large total ends exactly on its value.
 */
@Composable
internal fun animateCount(target: Long, durationMillis: Int): Long {
    var shown by remember { mutableLongStateOf(target) }
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (!hasAppeared) {
            shown = target
            hasAppeared = true
            return@LaunchedEffect
        }
        val start = shown
        animate(0f, 1f, animationSpec = tween(durationMillis, easing = LinearEasing)) { fraction, _ ->
            shown = countBetween(start, target, fraction)
        }
    }
    return shown
}

/** The number [fraction] of the way from [start] to [target]. */
internal fun countBetween(start: Long, target: Long, fraction: Float): Long =
    start + ((target - start) * fraction.toDouble()).roundToLong()
