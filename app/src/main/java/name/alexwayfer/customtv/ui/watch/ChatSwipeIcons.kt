package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText

/** What the reply circle turns to past the threshold: green, as a go-ahead, with a dark arrow. */
internal val ChatReplySwipeColors = ChatSwipeColors(background = Color(0xFF3DDC84), content = TwitchBg)

/** What the copy circle turns to past the threshold: blue, apart from the purple of a selected message. */
internal val ChatCopySwipeColors = ChatSwipeColors(background = Color(0xFF4C9AFF), content = TwitchBg)

/**
 * The reply arrow at the row's end while [swipe] slides it toward the start, and the copy icon at its start while it
 * slides toward the end and [copies]. At rest the icons sit clipped beyond the row's edges; rows not being swiped skip
 * them.
 */
@Composable
internal fun BoxScope.ReplyAndCopyIcons(
    swipe: ChatSwipe,
    copies: Boolean,
    thresholdPx: Float,
    circlePx: Float,
    towardStart: Float,
) {
    if (swipe.replyArrowShown) {
        SwipeIcon(
            R.drawable.ic_reply_arrow,
            atEnd = true,
            armedColors = ChatReplySwipeColors,
            armed = { chatReplySwipeReplies(swipe.offset.value, thresholdPx) },
            iconOffsetPx = { -towardStart * chatReplyArrowOffset(swipe.offset.value, thresholdPx, circlePx) },
        )
    }
    if (copies && swipe.towardEndShown) {
        SwipeIcon(
            R.drawable.ic_chat_copy,
            atEnd = false,
            armedColors = ChatCopySwipeColors,
            armed = { chatReplySwipeReplies(-swipe.offset.value, thresholdPx) },
            iconOffsetPx = { towardStart * chatReplyArrowOffset(-swipe.offset.value, thresholdPx, circlePx) },
        )
    }
}

/**
 * The swipe's [icon] in its circle at the row's end, or its start, shifted by [iconOffsetPx] as the row slides. The
 * circle takes [armedColors] while the row is [armed], past the threshold, where releasing it does the action.
 */
@Composable
private fun BoxScope.SwipeIcon(
    icon: Int,
    atEnd: Boolean,
    armedColors: ChatSwipeColors,
    armed: () -> Boolean,
    iconOffsetPx: () -> Float,
) {
    // Composes again only when the row crosses the threshold, not on each move of the finger.
    val isArmed by remember(armed) { derivedStateOf(armed) }
    val background by animateColorAsState(if (isArmed) armedColors.background else TwitchSurface, chatSwipeMotion())
    val content by animateColorAsState(if (isArmed) armedColors.content else TwitchText, chatSwipeMotion())
    val edgePadding = (CHAT_REPLY_SWIPE_THRESHOLD - CHAT_REPLY_SWIPE_CIRCLE_SIZE) / 2
    Box(
        modifier = Modifier
            .matchParentSize()
            .clipToBounds()
            .then(if (atEnd) Modifier.padding(end = edgePadding) else Modifier.padding(start = edgePadding)),
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                // A one-line row is lower than the circle: it shrinks to the row height and stays round.
                .sizeIn(maxWidth = CHAT_REPLY_SWIPE_CIRCLE_SIZE, maxHeight = CHAT_REPLY_SWIPE_CIRCLE_SIZE)
                .aspectRatio(1f)
                .graphicsLayer { translationX = iconOffsetPx() }
                .drawBehind { drawCircle(background) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(CHAT_REPLY_SWIPE_ICON_SIZE),
            )
        }
    }
}
