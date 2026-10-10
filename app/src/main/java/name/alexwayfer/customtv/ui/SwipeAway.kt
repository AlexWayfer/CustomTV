package name.alexwayfer.customtv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.ui.watch.ChatSwipeSettleTween
import name.alexwayfer.customtv.ui.watch.FULLSCREEN_CHAT_SWIPE_SHARE
import name.alexwayfer.customtv.ui.watch.MINIMIZE_FLING_VELOCITY
import kotlin.math.abs

/**
 * Whether letting go of a sideways swipe at [offsetX] with [velocityX] sends the swiped view away: past a third of
 * its width, or flung the way it already moved, as the full screen chat swipe takes it.
 */
internal fun swipeAwayDismisses(offsetX: Float, velocityX: Float, widthPx: Int): Boolean {
    if (offsetX == 0f) return false
    val past = abs(offsetX) > widthPx.coerceAtLeast(0) * FULLSCREEN_CHAT_SWIPE_SHARE
    val flung = abs(velocityX) > MINIMIZE_FLING_VELOCITY && (velocityX > 0f) == (offsetX > 0f)
    return past || flung
}

/** The swiped view fades as it nears its own width off its place. */
internal fun swipeAwayAlpha(offsetX: Float, widthPx: Int): Float {
    if (widthPx <= 0) return 1f
    return (1f - abs(offsetX) / widthPx).coerceIn(0f, 1f)
}

/** Lets [content] be swiped off sideways, then calls [onDismiss]; a short swipe springs back. */
@Composable
internal fun SwipeAway(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .graphicsLayer {
                translationX = offset.value
                alpha = swipeAwayAlpha(offset.value, widthPx)
            }
            .draggable(
                state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    if (swipeAwayDismisses(offset.value, velocity, widthPx)) {
                        val away = if (offset.value > 0f) widthPx.toFloat() else -widthPx.toFloat()
                        offset.animateTo(away, ChatSwipeSettleTween)
                        currentOnDismiss()
                    } else {
                        offset.animateTo(0f, ChatSwipeSettleTween)
                    }
                },
            ),
    ) {
        content()
    }
}
