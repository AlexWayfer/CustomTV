package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** Whether a fling frame that moves the list by [delta] has run into the end it moves toward. */
internal fun listEndStopsFling(delta: Float, canScrollForward: Boolean, canScrollBackward: Boolean): Boolean =
    if (delta > 0f) !canScrollForward else delta < 0f && !canScrollBackward

/** A list's [connection] for `Modifier.nestedScroll` and its [flingBehavior], from [rememberListEndFlingStop]. */
internal class ListEndFlingStop(val connection: NestedScrollConnection, val flingBehavior: FlingBehavior)

/**
 * For a list inside a scrolled page: a fling that reaches the list's end stops there instead of carrying the page on,
 * as the extension's lists do with `overscroll-behavior: contain`. A drag past the end still moves the page. The rest
 * of the fling is dropped, not handed to the list's edge effect: while that edge stretches, the list takes the next
 * touch as a drag, and a tap on a chatter at the end would do nothing.
 */
@Composable
internal fun rememberListEndFlingStop(state: LazyListState): ListEndFlingStop {
    val decay = rememberSplineBasedDecay<Float>()
    val flingBehavior = remember(decay, state) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(decay) {
                    val delta = value - lastValue
                    lastValue = value
                    scrollBy(delta)
                    velocityLeft = velocity
                    // The connection takes what the list cannot, which the default fling counts as scrolled: it
                    // would run on against the end, and the next tap would only stop it.
                    if (listEndStopsFling(delta, state.canScrollForward, state.canScrollBackward)) cancelAnimation()
                }
                return velocityLeft
            }
        }
    }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (source == NestedScrollSource.UserInput) Offset.Zero else Offset(0f, available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(0f, available.y)
        }
    }
    return remember(connection, flingBehavior) { ListEndFlingStop(connection, flingBehavior) }
}
