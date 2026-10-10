package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.ui.theme.TwitchPurple

/** The overlay of a selected chat message; it fades in and out rather than switching at once. */
@Composable
internal fun animatedSelectionOverlay(selected: Boolean, overlayAlpha: Float): Color =
    // Fades the same purple in and out, so the overlay does not pass through a darker shade.
    TwitchPurple.copy(alpha = animatedRowValue(if (selected) overlayAlpha else 0f, rest = 0f))

/** The alpha a message's content dims to once it is deleted, reached gradually. */
@Composable
internal fun animatedContentAlpha(alpha: Float): Float = animatedRowValue(alpha, rest = 1f)

/**
 * Springs toward [target] like `animateFloatAsState`. Every row entering view calls this, and nearly every row
 * stays at [rest]; such a row runs no effect until its target leaves [rest].
 */
@Composable
private fun animatedRowValue(target: Float, rest: Float): Float {
    val value = remember { Animatable(target) }
    if (target != rest || value.value != rest) {
        LaunchedEffect(target) { value.animateTo(target) }
    }
    return value.value
}
