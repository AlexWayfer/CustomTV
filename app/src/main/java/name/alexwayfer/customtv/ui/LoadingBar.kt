package name.alexwayfer.customtv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.watch.HeaderEmphasizedAccelerate
import name.alexwayfer.customtv.ui.watch.HeaderEmphasizedDecelerate

internal val LOADING_BAR_HEIGHT = 4.dp

/** The width the bar grows from, and the width a released pull indicator stretches to as it flows into it. */
internal val LOADING_BAR_START_WIDTH = 80.dp

/** How long the bar takes to appear or fade. */
private const val LOADING_BAR_MOTION_MS = 300

/** How long a load the app starts by itself runs before the bar shows: a shorter one passes without it. */
private val LOADING_BAR_DELAY = 200.milliseconds

/** Whether the loading bar is on screen, and since when. */
@Stable
internal class LoadingBarState {
    var shownAt by mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null)
        private set

    val visible: Boolean get() = shownAt != null

    /** Shows the bar without the delay, for a load the user started. */
    fun showNow() {
        if (shownAt == null) shownAt = TimeSource.Monotonic.markNow()
    }

    fun hide() {
        shownAt = null
    }
}

/** The bar for [loading]: shown after the delay, and kept for the minimum time once shown. */
@Composable
internal fun rememberLoadingBarState(loading: Boolean): LoadingBarState {
    val state = remember { LoadingBarState() }
    LaunchedEffect(loading, state.visible) {
        val shownAt = state.shownAt
        if (loading && shownAt == null) {
            delay(LOADING_BAR_DELAY)
            state.showNow()
        } else if (!loading && shownAt != null) {
            delay(progressHideDelay(shownAt.elapsedNow()))
            state.hide()
        }
    }
    return state
}

/**
 * A thin indeterminate bar along a container's edge, growing out from its center. While [held], it waits to grow,
 * so a pull indicator can first flow into its place.
 */
@Composable
internal fun LoadingBar(state: LoadingBarState, modifier: Modifier = Modifier, held: Boolean = false) {
    val startWidthPx = with(LocalDensity.current) { LOADING_BAR_START_WIDTH.roundToPx() }
    val visible = state.visible && !held
    // It starts in the pull indicator's color, the color of what flows into it, and turns into the running
    // indicator while it grows.
    val growColor = PullToRefreshDefaults.indicatorContainerColor
    val solid = remember { Animatable(1f) }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        solid.snapTo(1f)
        solid.animateTo(0f, tween(LOADING_BAR_MOTION_MS, easing = HeaderEmphasizedDecelerate))
    }
    // The width belongs to the box, not to the bar: a full minimum width on the bar itself would hold it at full
    // width from the first frame, so it could not grow out from the center.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LOADING_BAR_MOTION_MS, easing = HeaderEmphasizedDecelerate)) +
                expandHorizontally(
                    animationSpec = tween(LOADING_BAR_MOTION_MS, easing = HeaderEmphasizedDecelerate),
                    expandFrom = Alignment.CenterHorizontally,
                    initialWidth = { startWidthPx },
                ),
            exit = fadeOut(tween(LOADING_BAR_MOTION_MS, easing = HeaderEmphasizedAccelerate)),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LOADING_BAR_HEIGHT)
                    .drawWithContent {
                        drawContent()
                        drawRect(growColor, alpha = solid.value)
                    },
                color = TwitchPurple,
                trackColor = TwitchDivider,
                gapSize = 0.dp,
            )
        }
    }
}
