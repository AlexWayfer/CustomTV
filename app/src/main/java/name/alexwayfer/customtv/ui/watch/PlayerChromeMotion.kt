package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/** How long the stream info panel takes to open or close; what moves with it takes as long. */
internal const val HEADER_SIZE_MS = 400
internal const val HEADER_FADE_IN_MS = 180
internal const val HEADER_FADE_OUT_MS = 120
internal val HeaderEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
internal val HeaderEmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** The panel opens and closes from the first frame and settles at the end, so no empty room hangs open. */
internal val HeaderPanelEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** The fade of the panel's details, for the player chrome that shows and hides with them. */
internal val HeaderFadeIn: EnterTransition = fadeIn(tween(HEADER_FADE_IN_MS, easing = LinearOutSlowInEasing))
internal val HeaderFadeOut: ExitTransition = fadeOut(tween(HEADER_FADE_OUT_MS, easing = FastOutLinearInEasing))
