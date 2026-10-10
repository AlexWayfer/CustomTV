package name.alexwayfer.customtv.ui.watch

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.CHAT_WELCOME_NOTICE_ID
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.ui.theme.TwitchBg
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** How tall a chat laid over a full screen video is. */
internal enum class FullscreenOverlayChatHeight {
    /** The latest few messages in the bottom corner. */
    Compact,

    /** The whole chat on its side of the video. */
    Full,

    /** Not laid over the video, or open for typing: the chat takes its full column and keeps its message bar. */
    None,
}

/**
 * An overlaid chat shows as its latest messages in the corner until the user expands it. While the user types it
 * takes the full height beside the video, as before.
 */
internal fun fullscreenOverlayChatHeight(
    overPlayer: Boolean,
    expanded: Boolean,
    composerOpen: Boolean,
): FullscreenOverlayChatHeight = when {
    !overPlayer || composerOpen -> FullscreenOverlayChatHeight.None
    expanded -> FullscreenOverlayChatHeight.Full
    else -> FullscreenOverlayChatHeight.Compact
}

/**
 * The messages the compact chat may show: a live chat's from its first notice on, so recent chat loaded from
 * elsewhere, which can land after the first live messages, never shows there. A chat without that notice, such as a
 * replay, shows all of them.
 */
internal fun compactChatCandidates(messages: List<ChatMessage>): List<ChatMessage> {
    val start = messages.indexOfFirst { it.id == CHAT_WELCOME_NOTICE_ID }
    return if (start < 0) messages else messages.subList(start, messages.size)
}

/**
 * When each of [ids] came: the time from [previous], or [nowMillis] for a new one. On the chat's first look
 * ([previous] null) they were there before it, such as recent chat or an earlier list, and count as gone already,
 * [lifetimeMillis] ago. Others are dropped.
 */
internal fun compactChatShownAt(
    previous: Map<String, Long>?,
    ids: List<String>,
    nowMillis: Long,
    lifetimeMillis: Long,
): Map<String, Long> = ids.associateWith { id ->
    if (previous == null) nowMillis - lifetimeMillis else previous[id] ?: nowMillis
}

/**
 * Which of [ids] still show at [nowMillis]: each one hides [lifetimeMillis] after it came. One not yet in [shownAt]
 * waits until it is, so a message that was there before the chat looked never flashes in.
 */
internal fun compactChatVisibleIds(
    ids: List<String>,
    shownAt: Map<String, Long>,
    nowMillis: Long,
    lifetimeMillis: Long,
): Set<String> = ids.filterTo(LinkedHashSet()) { id ->
    val cameAt = shownAt[id]
    cameAt != null && nowMillis - cameAt < lifetimeMillis
}

/** When the next of the messages that came at [shownAt] hides; null when every one has hidden already. */
internal fun compactChatNextHideMillis(shownAt: Map<String, Long>, nowMillis: Long, lifetimeMillis: Long): Long? =
    shownAt.values.map { it + lifetimeMillis }.filter { it > nowMillis }.minOrNull()

/** The top message that fades out toward its top edge: only once the corner shows as many as it holds. */
internal fun compactChatFadedId(ids: List<String>, visible: Set<String>, capacity: Int): String? =
    if (visible.size >= capacity) ids.firstOrNull { it in visible } else null

/** How tall the message bar is while it folds away under an overlaid chat; never below zero. */
internal fun foldedHeightPx(heightPx: Int, shown: Float): Int =
    (heightPx.coerceAtLeast(0) * shown.coerceIn(0f, 1f)).roundToInt()

/**
 * The latest few messages of a chat laid over the video, each on its own see-through ground. Each one hides a while
 * after it came ([shownAt], kept by the chat while the corner is gone, so typing does not show the same messages
 * again), even without newer ones. A tap expands the chat to its full height ([onExpand]); the rows themselves take
 * no taps.
 */
@Composable
internal fun FullscreenCompactChat(
    messages: List<ChatMessage>,
    shownAt: Map<String, Long>,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (ChatMessage) -> Unit,
) {
    // A few more than show, so the messages that newer ones push out fold away instead of vanishing.
    val kept = uniqueChatRows(messages.takeLast(COMPACT_CHAT_MESSAGES * 2))
    val shownIds = kept.takeLast(COMPACT_CHAT_MESSAGES).map { it.id }
    var nowMillis by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(shownAt) {
        nowMillis = SystemClock.uptimeMillis()
        while (true) {
            val hideAt = compactChatNextHideMillis(shownAt, nowMillis, COMPACT_CHAT_MESSAGE_MILLIS) ?: break
            delay((hideAt - SystemClock.uptimeMillis()).coerceAtLeast(0L).milliseconds)
            nowMillis = SystemClock.uptimeMillis()
        }
    }
    val visible = compactChatVisibleIds(shownIds, shownAt, nowMillis, COMPACT_CHAT_MESSAGE_MILLIS)
    val fadedId = compactChatFadedId(shownIds, visible, COMPACT_CHAT_MESSAGES)
    // Messages already showing when the corner comes back stand as the full chat turned into them while it sank;
    // later ones come in from below.
    val firstComposition = remember { FirstComposition() }
    SideEffect { firstComposition.done = true }
    val currentOnExpand by rememberUpdatedState(onExpand)
    val expandLabel = stringResource(R.string.fullscreen_chat_expand)
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = CompactChatSideGap)) {
            kept.forEach { message ->
                key(message.id) {
                    val visibleState = remember {
                        MutableTransitionState(!firstComposition.done && message.id in visible)
                    }
                    visibleState.targetState = message.id in visible
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(tween(COMPACT_CHAT_MOTION_MS)) +
                            expandVertically(tween(COMPACT_CHAT_MOTION_MS), expandFrom = Alignment.Top),
                        // A message leaves upward, out of the corner the chat grows from.
                        exit = fadeOut(tween(COMPACT_CHAT_MOTION_MS)) +
                            slideOutVertically(tween(COMPACT_CHAT_MOTION_MS)) { -it } +
                            shrinkVertically(tween(COMPACT_CHAT_MOTION_MS), shrinkTowards = Alignment.Bottom),
                    ) {
                        val topAlpha = animateFloatAsState(
                            targetValue = if (message.id == fadedId) COMPACT_CHAT_FADED_TOP_ALPHA else 1f,
                            animationSpec = tween(COMPACT_CHAT_MOTION_MS),
                            label = "compact chat fade",
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = CompactChatBubbleGap)
                                .fadedTop { topAlpha.value }
                                .clip(CompactChatBubbleShape)
                                .background(TwitchBg.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA)),
                        ) {
                            row(message)
                        }
                    }
                }
            }
        }
        // Over the rows, so a tap expands the chat instead of opening a message's menu.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures { currentOnExpand() } }
                .semantics {
                    onClick(label = expandLabel) {
                        currentOnExpand()
                        true
                    }
                },
        )
    }
}

/** Whether the composable already went through its first composition; read only while composing. */
private class FirstComposition {
    var done = false
}

/** Fades the content toward its top edge, down to [topAlpha] there; read when drawn. */
internal fun Modifier.fadedTop(topAlpha: () -> Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val top = topAlpha()
            if (top < 1f) {
                drawRect(
                    brush = Brush.verticalGradient(0f to Color.Black.copy(alpha = top), 1f to Color.Black),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

/**
 * Folds the message bar away from its top while [shown] goes from 1 to 0, clipped only while folded, so the
 * suggestions above an open bar still show.
 */
internal fun Modifier.foldedAway(shown: () -> Float): Modifier =
    graphicsLayer {
        val fraction = shown()
        clip = fraction < 1f
        alpha = fraction
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = foldedHeightPx(placeable.height, shown())
        layout(placeable.width, height) {
            placeable.place(0, height - placeable.height)
        }
    }

/** How many of the latest messages the compact chat shows. */
internal const val COMPACT_CHAT_MESSAGES = 3

/** How long a message stays in the compact chat. */
internal const val COMPACT_CHAT_MESSAGE_MILLIS = 7_000L

internal const val COMPACT_CHAT_FADED_TOP_ALPHA = 0.35f
internal const val COMPACT_CHAT_MOTION_MS = 220
internal val CompactChatBubbleCorner = 8.dp
internal val CompactChatBubbleShape = RoundedCornerShape(CompactChatBubbleCorner)

/** The room beside the messages of the compact chat. */
internal val CompactChatSideGap = 8.dp

/** The room above each message of the compact chat. */
internal val CompactChatBubbleGap = 6.dp

/** How far a chat laid over the video keeps from the screen's top and bottom edges. */
internal val OverlayChatEdgeGap = 16.dp
