package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.ui.LOADING_BAR_HEIGHT
import name.alexwayfer.customtv.ui.LOADING_BAR_START_WIDTH
import name.alexwayfer.customtv.ui.LoadingBar
import name.alexwayfer.customtv.ui.rememberLoadingBarState

/** How far the refresh indicator comes down: its 40dp below an 8dp gap at the top. */
private val REFRESH_INDICATOR_DISTANCE = 48.dp

/**
 * Pull to refresh over a list that never moves. The pulled indicator comes down over the list, and on release
 * it flows into a thin bar, in the band under the title on top of the list when it has one ([headerHeightPx]
 * tall), or along the top edge otherwise. The bar lies over the list, so nothing moves when it shows. It also
 * shows a refresh the app starts by itself.
 */
@Composable
internal fun ListPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    headerHeightPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val barTopPx = with(density) {
        listLoadingBarTop(headerHeightPx, SECTION_HEADER_BAND.roundToPx(), LOADING_BAR_HEIGHT.roundToPx())
    }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val bar = rememberLoadingBarState(isRefreshing)
    // Where the indicator was released while it flows into the bar; null when nothing is flowing.
    var releasedFraction by remember { mutableStateOf<Float?>(null) }
    val morph = remember { Animatable(0f) }
    val barHeld by remember {
        derivedStateOf { releasedFraction != null && morph.value < PULL_INDICATOR_OPAQUE_PART }
    }
    PullToRefreshBox(
        // The indicator never stays down: the bar shows the refresh instead.
        isRefreshing = false,
        onRefresh = {
            releasedFraction = pullState.distanceFraction
            bar.showNow()
            scope.launch {
                morph.snapTo(0f)
                morph.animateTo(1f, tween(PULL_INDICATOR_FLOW_MS, easing = FastOutSlowInEasing))
                releasedFraction = null
            }
            if (!isRefreshing) onRefresh()
        },
        modifier = modifier.clipToBounds(),
        state = pullState,
        indicator = {
            val released = releasedFraction
            if (released == null) {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = false,
                    modifier = Modifier.align(Alignment.TopCenter),
                    maxDistance = REFRESH_INDICATOR_DISTANCE,
                )
            } else {
                val heldState = remember(released) { HeldPullToRefreshState(released) }
                PullToRefreshDefaults.Indicator(
                    state = heldState,
                    isRefreshing = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val progress = morph.value
                            val center = pullIndicatorCenter(
                                distanceFraction = released,
                                maxDistancePx = REFRESH_INDICATOR_DISTANCE.roundToPx().toFloat(),
                                heightPx = size.height,
                            )
                            // Stretches to the width the bar grows from and flattens into its thickness, around
                            // its own center while that center moves onto the bar's, so it becomes the bar rather
                            // than vanishing into a point.
                            transformOrigin = TransformOrigin(0.5f, center / size.height)
                            translationY = (barTopPx + LOADING_BAR_HEIGHT.toPx() / 2 - center) * progress
                            scaleX = 1f + progress * (LOADING_BAR_START_WIDTH.toPx() / size.width - 1f)
                            scaleY = 1f - progress * (1f - LOADING_BAR_HEIGHT.toPx() / size.height)
                            alpha = pullIndicatorFlowAlpha(progress)
                        },
                    maxDistance = REFRESH_INDICATOR_DISTANCE,
                )
            }
        },
    ) {
        content()
        LoadingBar(
            bar,
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, barTopPx) },
            // The released indicator lies down in the bar's place first; the bar then grows out of it while it fades.
            held = barHeld,
        )
    }
}

/** How long the released indicator takes to lie down in the bar's place and fade. */
private const val PULL_INDICATOR_FLOW_MS = 450

/**
 * How much of the flow the released indicator stays fully opaque for, so it is seen lying down before it fades.
 * The bar starts growing out of it there.
 */
private const val PULL_INDICATOR_OPAQUE_PART = 0.6f

/** The released indicator's opacity at [progress] of its flow into the bar: full, then fading out at the end. */
internal fun pullIndicatorFlowAlpha(progress: Float): Float =
    ((1f - progress) / (1f - PULL_INDICATOR_OPAQUE_PART)).coerceIn(0f, 1f)

/**
 * Where the loading bar's top goes: centered in the [bandPx] band along the bottom of a title [headerHeightPx]
 * tall, or at the top edge when the list has no title on top.
 */
internal fun listLoadingBarTop(headerHeightPx: Int, bandPx: Int, barPx: Int): Int =
    if (headerHeightPx <= 0) 0 else (headerHeightPx - (bandPx + barPx) / 2).coerceAtLeast(0)

/** The pulled indicator held still where it was released, while it flows into the bar. */
private class HeldPullToRefreshState(override val distanceFraction: Float) : PullToRefreshState {
    override val isAnimating: Boolean = false

    override suspend fun animateToThreshold() = Unit

    override suspend fun animateToHidden() = Unit

    override suspend fun snapTo(targetValue: Float) = Unit
}

/** The center of the Material pull indicator below the top, the way it places itself at [distanceFraction]. */
internal fun pullIndicatorCenter(distanceFraction: Float, maxDistancePx: Float, heightPx: Float): Float =
    distanceFraction * maxDistancePx - heightPx / 2
