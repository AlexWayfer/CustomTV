package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The stream info runs under the video's part of the area: beside a column chat, or the whole width. */
internal fun fullscreenInfoBarSpan(bounds: FullscreenChatBounds?, areaWidthPx: Int): FullscreenInfoBarSpan {
    val area = areaWidthPx.coerceAtLeast(0)
    return FullscreenInfoBarSpan(x = bounds?.playerX ?: 0, width = (bounds?.playerWidth ?: area).coerceIn(0, area))
}

/**
 * The top of the stream info [barHeightPx] tall in an area [areaHeightPx] tall: on the bottom while [shown], under
 * the bottom edge while hidden, and up with the stream as it hides ([hidden] from 0 to 1), right under its video.
 */
internal fun fullscreenInfoBarTopPx(areaHeightPx: Int, barHeightPx: Int, shown: Float, hidden: Float): Int {
    val rest = (areaHeightPx - barHeightPx * shown.coerceIn(0f, 1f)).coerceAtLeast(0f)
    return (rest * (1f - hidden.coerceIn(0f, 1f))).roundToInt()
}

/**
 * The stream info shows under a full screen video with the player's [controls], making way for the keyboard; it keeps
 * its room while the video [covers] the screen on its way out of full screen or back.
 */
internal fun fullscreenInfoBarShown(controls: Boolean, composerOpen: Boolean, fullscreen: Boolean, covers: Boolean) =
    controls && !composerOpen && (fullscreen || covers)

/** How far the stream info under a full screen video is in sight, from 0 to 1; read when laid out or drawn. */
@Composable
internal fun animateFullscreenInfoBarShown(shown: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (shown) 1f else 0f,
    animationSpec = tween(HEADER_SIZE_MS, easing = HeaderPanelEasing),
    label = "full screen info",
)

/**
 * Lays the stream info out across [span] of the area at its own height, which it reports ([onHeight]), and places it
 * as [fullscreenInfoBarTopPx] says; read when laid out.
 */
internal fun Modifier.fullscreenInfoBar(
    span: (areaWidthPx: Int) -> FullscreenInfoBarSpan,
    shown: () -> Float,
    hidden: () -> Float,
    onHeight: (Int) -> Unit,
): Modifier = layout { measurable, constraints ->
    val at = span(constraints.maxWidth)
    val width = at.width.coerceIn(0, constraints.maxWidth)
    val placeable = measurable.measure(
        Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight),
    )
    onHeight(placeable.height)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(at.x, fullscreenInfoBarTopPx(constraints.maxHeight, placeable.height, shown(), hidden()))
    }
}

/**
 * The stream info under a full screen video, shown with its controls: the video shrinks above it at the same pace.
 * It stays while the stream slides out of full screen from it, so a swipe up on it keeps going.
 */
@Composable
internal fun FullscreenInfoBar(
    scene: PlayerScene,
    frame: PlayerFrame,
    layout: FullscreenChatLayout,
    content: @Composable () -> Unit,
) {
    val hiding = scene.streamHiding
    if (!scene.fullscreenActive && hiding?.keepsFullscreenInfo != true) return
    // The chat beside the video at rest, not one a swipe is still bringing in.
    val bounds = if (scene.fullscreenActive) {
        fullscreenChatBounds(scene.fullscreen.chatMode, scene.fullscreen.chatSide, layout.areaWidthPx)
    } else {
        null
    }
    val ui = scene.ui
    Box(
        modifier = Modifier
            .fillMaxSize()
            .fullscreenDragMotion(scene.fullscreen, scene.screenHeightPx)
            .padding(horizontal = frame.fullscreenSide)
            .windowInsetsPadding(frame.cutout.only(WindowInsetsSides.Vertical))
            .graphicsLayer {
                translationX = fullscreenPlayerSwipeShiftPx(scene.chatSwipe.offsetX, layout.chat, layout.swipeTravelPx)
            },
    ) {
        Box(
            modifier = Modifier
                .fullscreenInfoBar(
                    span = { width -> fullscreenInfoBarSpan(bounds, width) },
                    shown = { scene.infoBarShown.value },
                    hidden = { hiding?.fraction ?: 0f },
                    onHeight = { ui.fullscreenInfoPx = it },
                )
                // Out of sight under the bottom edge, TalkBack does not reach it.
                .then(if (scene.infoBarShownTarget) Modifier else Modifier.clearAndSetSemantics {}),
        ) {
            content()
        }
    }
}

/**
 * How far the buttons in the full screen player's top corner sit below its top: their middles line up with where
 * the stream info's heart and bell used to be (a 10.dp top padding plus half of a 40.dp button, less half of a 44.dp
 * one).
 */
internal val FullscreenTopButtonsTop = 8.dp
