package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText

/** How much farther each step of a swipe toward the end lies than the one before; the first is at the threshold. */
internal val CHAT_SWIPE_STEP_GAP = 48.dp

private val CHAT_SWIPE_STEP_LABEL_SIZE = 14.sp
private val CHAT_SWIPE_STEP_LABEL_END_PADDING = 12.dp

/**
 * How long a swipe's circle or plaque takes to take the color of what releasing it does, and the plaque to turn into
 * the next step's: its icon, name, size, and colors together.
 */
private const val CHAT_SWIPE_MOTION_MILLIS = 220

internal fun <T> chatSwipeMotion() = tween<T>(CHAT_SWIPE_MOTION_MILLIS, easing = FastOutSlowInEasing)

/** The color of a swipe's circle or plaque, and of its icon and name on it. */
internal class ChatSwipeColors(val background: Color, val content: Color)

/** What a step of a swipe toward the end shows while the row stands on it. */
internal class ChatSwipeStepLook(val icon: Painter, val label: String, val colors: ChatSwipeColors)

/**
 * The [count] steps a swipe toward the end goes through, each farther than the one before: [look] tells what a step
 * shows, and releasing the row on one calls [onStep] with its index.
 */
internal class ChatSwipeSteps(
    val count: Int,
    val look: @Composable (Int) -> ChatSwipeStepLook,
    val onStep: (Int) -> Unit,
)

/**
 * What a swipe toward the end does for a row that [dismisses] when selected, has [stepCount] steps, or [copies]: in
 * that order, with the steps [stepGapPx] apart.
 */
internal fun chatSwipeTowardEnd(
    dismisses: Boolean,
    stepCount: Int,
    copies: Boolean,
    stepGapPx: Float,
): ChatSwipeTowardEnd = when {
    dismisses -> ChatSwipeTowardEnd.Dismiss
    stepCount > 0 -> ChatSwipeTowardEnd.Steps(stepCount, stepGapPx)
    copies -> ChatSwipeTowardEnd.Copy
    else -> ChatSwipeTowardEnd.None
}

/** The step [travelPx] toward the end has reached: -1 short of the first, at [thresholdPx]; the last past its place. */
internal fun chatSwipeStep(travelPx: Float, thresholdPx: Float, steps: ChatSwipeTowardEnd.Steps): Int {
    if (steps.count <= 0 || thresholdPx <= 0f || travelPx < thresholdPx) return -1
    if (steps.gapPx <= 0f) return 0
    return ((travelPx - thresholdPx) / steps.gapPx).toInt().coerceAtMost(steps.count - 1)
}

/** Where the last of [steps] lies, as travel toward the end. */
internal fun chatSwipeLastStepPx(thresholdPx: Float, steps: ChatSwipeTowardEnd.Steps): Float =
    thresholdPx + (steps.count - 1).coerceAtLeast(0) * steps.gapPx

/**
 * Which stage of the swipe [dragPx] of signed travel stands on: 0 where a release does nothing, 1 where it does the
 * one action of its side, and with [ChatSwipeTowardEnd.Steps] the step's number, counting from 1. The row ticks as
 * it enters a new stage.
 */
internal fun chatSwipeStage(dragPx: Float, thresholdPx: Float, towardEnd: ChatSwipeTowardEnd): Int = when {
    chatSwipeRelease(dragPx, thresholdPx, towardEnd) == ChatSwipeRelease.Back -> 0
    dragPx < 0f && towardEnd is ChatSwipeTowardEnd.Steps -> chatSwipeStep(-dragPx, thresholdPx, towardEnd) + 1
    else -> 1
}

/**
 * The step a swipe toward the end stands on, by the row's slide [offsetPx], at the row's start: its icon in the
 * circle, and its name beside it on the step's color once the row reached it; short of the first step, the first
 * one's icon slides in alone on the plain circle, shifted by [iconOffsetPx].
 */
@Composable
internal fun BoxScope.SwipeStepIndicator(
    steps: ChatSwipeSteps,
    towardEnd: ChatSwipeTowardEnd.Steps,
    thresholdPx: Float,
    offsetPx: () -> Float,
    iconOffsetPx: () -> Float,
) {
    // Composes again only when the step changes, not on each move of the finger.
    val step by remember(towardEnd, thresholdPx, offsetPx) {
        derivedStateOf { chatSwipeStep(-offsetPx(), thresholdPx, towardEnd) }
    }
    val colors = steps.look(step.coerceAtLeast(0)).colors.takeIf { step >= 0 }
    val background by animateColorAsState(colors?.background ?: TwitchSurface, chatSwipeMotion())
    val content by animateColorAsState(colors?.content ?: TwitchText, chatSwipeMotion())
    val edgePadding = (CHAT_REPLY_SWIPE_THRESHOLD - CHAT_REPLY_SWIPE_CIRCLE_SIZE) / 2
    Box(
        modifier = Modifier
            .matchParentSize()
            .clipToBounds()
            .padding(start = edgePadding)
            // Only a swipe shows it; the message menu names the same actions for TalkBack.
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterStart,
    ) {
        AnimatedContent(
            targetState = step,
            modifier = Modifier
                .graphicsLayer { translationX = iconOffsetPx() }
                .heightIn(max = CHAT_REPLY_SWIPE_CIRCLE_SIZE)
                .drawBehind { drawRoundRect(background, cornerRadius = CornerRadius(size.height / 2)) },
            transitionSpec = {
                fadeIn(chatSwipeMotion()) togetherWith fadeOut(chatSwipeMotion()) using
                    SizeTransform { _, _ -> chatSwipeMotion() }
            },
        ) { shown ->
            SwipeStepLook(steps.look(shown.coerceAtLeast(0)), labelShown = shown >= 0, contentColor = { content })
        }
    }
}

/** The step's icon and name, in [contentColor], which the plaque animates along with its own color. */
@Composable
private fun SwipeStepLook(look: ChatSwipeStepLook, labelShown: Boolean, contentColor: () -> Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .sizeIn(maxWidth = CHAT_REPLY_SWIPE_CIRCLE_SIZE, maxHeight = CHAT_REPLY_SWIPE_CIRCLE_SIZE)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = look.icon,
                contentDescription = null,
                tint = contentColor(),
                modifier = Modifier.size(CHAT_REPLY_SWIPE_ICON_SIZE),
            )
        }
        if (labelShown) {
            Text(
                text = look.label,
                color = contentColor(),
                fontSize = CHAT_SWIPE_STEP_LABEL_SIZE,
                maxLines = 1,
                modifier = Modifier.padding(end = CHAT_SWIPE_STEP_LABEL_END_PADDING),
            )
        }
    }
}
