package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatThreadEntry
import name.alexwayfer.customtv.chat.asChatMessage
import name.alexwayfer.customtv.chat.copyableTextWithAuthor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.chat.replySessionShowsReplyingTo
import name.alexwayfer.customtv.chat.threadPanelDragCloses
import name.alexwayfer.customtv.chat.threadPinnedMaxHeightPx
import name.alexwayfer.customtv.chat.threadWindow
import name.alexwayfer.customtv.chat.withDisplayParts
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

private const val THREAD_MOTION_MILLIS = 220
private const val THREAD_SELECTED_OVERLAY_ALPHA = 0.24f
private const val DELETED_MESSAGE_ALPHA = 0.42f

@Composable
internal fun ChatThreadPanel(
    session: ChatReplySession,
    onChoose: (String) -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    appearance: ChatAppearance,
) {
    val starter = session.entries.firstOrNull { it.id == session.starterId }
    val target = session.entries.firstOrNull { it.id == session.targetId }
    val starterName = starter?.displayName?.ifBlank { starter.login }.orEmpty()
    var selectedId by remember(session.starterId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val onCloseState = rememberUpdatedState(onClose)
    val closeSlopPx = with(LocalDensity.current) { 72.dp.toPx() }
    var panelHeight by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }
    val animatedOffset = remember { Animatable(0f) }
    val offset = if (settling) animatedOffset.value else drag
    val settlingState = rememberUpdatedState(settling)
    // Rises from the input as it opens, the way it slides back down when it closes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(THREAD_MOTION_MILLIS, easing = FastOutSlowInEasing)) }
    fun dismiss(fromDrag: Boolean) {
        if (settling) return
        val amount = drag
        if (fromDrag && !threadPanelDragCloses(amount, panelHeight, closeSlopPx)) {
            settling = true
            scope.launch {
                animatedOffset.snapTo(amount)
                animatedOffset.animateTo(0f, tween(THREAD_MOTION_MILLIS, easing = FastOutSlowInEasing))
                drag = 0f
                settling = false
            }
            return
        }
        settling = true
        scope.launch {
            animatedOffset.snapTo(amount)
            val targetOffset = panelHeight.toFloat().coerceAtLeast(1f)
            animatedOffset.animateTo(targetOffset, tween(THREAD_MOTION_MILLIS, easing = FastOutSlowInEasing))
            onCloseState.value()
        }
    }
    // Stays enabled while the panel slides away, so a second Back does not minimize the player.
    BackHandler { dismiss(fromDrag = false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .threadSwipeDown(offset) { reveal.value }
            .onSizeChanged { panelHeight = it.height }
            .background(TwitchSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dy ->
                            if (!settlingState.value) {
                                change.consume()
                                drag = (drag + dy).coerceAtLeast(0f)
                            }
                        },
                        onDragCancel = { dismiss(fromDrag = true) },
                        onDragEnd = { dismiss(fromDrag = true) },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chat_thread_with, starterName),
                color = TwitchText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
            IconButton(onClick = { dismiss(fromDrag = false) }, enabled = !settling) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.chat_close_thread),
                    tint = TwitchTextSecondary,
                )
            }
        }
        val window = threadWindow(session.entries, session.starterId)
        val replyScroll = rememberScrollState()
        val pinnedScroll = rememberScrollState()
        // A row being swiped to reply holds the replies still, so a diagonal swipe does not scroll them.
        var replySwiping by remember { mutableStateOf(false) }
        var pinnedContentPx by remember { mutableIntStateOf(0) }
        var followingContentPx by remember { mutableIntStateOf(0) }
        LaunchedEffect(window.following.map { it.id }) {
            snapshotFlow { replyScroll.maxValue }.collect { max ->
                replyScroll.animateScrollTo(max, tween(THREAD_MOTION_MILLIS, easing = FastOutSlowInEasing))
            }
        }
        // Weighted, so the title and the Replying to row are measured first and stay whole when the keyboard leaves little room.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 280.dp),
        ) {
            val pinnedMaxPx = threadPinnedMaxHeightPx(
                availablePx = constraints.maxHeight,
                pinnedPx = pinnedContentPx,
                followingPx = if (window.following.isEmpty()) 0 else followingContentPx,
            )
            Column {
            window.pinned?.let { entry ->
                ChatThreadMessages(
                    entries = listOf(entry),
                    selectedId = selectedId,
                    appearance = appearance,
                    onSelect = { selectedId = it },
                    onDismissSelection = { if (selectedId == it) selectedId = null },
                    onReply = { messageId ->
                        selectedId = null
                        onChoose(messageId)
                    },
                    onCopy = { selectedId = null },
                    onReplySwipingChange = { replySwiping = it },
                    modifier = Modifier
                        .heightIn(max = with(LocalDensity.current) { pinnedMaxPx.toDp() })
                        .verticalScroll(pinnedScroll, enabled = !replySwiping)
                        .onSizeChanged { pinnedContentPx = it.height },
                )
            }
            if (window.following.isNotEmpty()) {
                ChatThreadMessages(
                    entries = window.following,
                    selectedId = selectedId,
                    appearance = appearance,
                    onSelect = { selectedId = it },
                    onDismissSelection = { if (selectedId == it) selectedId = null },
                    onReply = { messageId ->
                        selectedId = null
                        onChoose(messageId)
                    },
                    onCopy = { selectedId = null },
                    onReplySwipingChange = { replySwiping = it },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(replyScroll, enabled = !replySwiping)
                        .onSizeChanged { followingContentPx = it.height },
                )
            }
            }
        }
        if (replySessionShowsReplyingTo(session)) {
            val name = target?.displayName?.ifBlank { target.login }.orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TwitchSurfaceAlt)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.chat_replying_to, name),
                    color = TwitchText,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), color = TwitchPurple)
                }
            }
        }
    }
}

@Composable
private fun ChatThreadMessages(
    entries: List<ChatThreadEntry>,
    selectedId: String?,
    appearance: ChatAppearance,
    onSelect: (String) -> Unit,
    onDismissSelection: (String) -> Unit,
    onReply: (String) -> Unit,
    onCopy: () -> Unit,
    onReplySwipingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        entries.forEach { entry ->
            ChatThreadRow(
                entry = entry,
                selected = selectedId == entry.id,
                appearance = appearance,
                onSelect = { onSelect(entry.id) },
                onDismissSelection = { onDismissSelection(entry.id) },
                onReply = { onReply(entry.id) },
                onCopy = onCopy,
                onReplySwipingChange = onReplySwipingChange,
            )
        }
    }
}

private fun Modifier.threadSwipeDown(offsetPx: Float, reveal: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shown = threadPanelShownHeightPx(placeable.height, offsetPx, reveal())
    layout(placeable.width, shown) {
        placeable.place(0, 0)
    }
}

/**
 * The height the thread panel takes while it is dragged down by [dragPx] and opened as far as [reveal]
 * (0 closed, 1 open). Never below zero, nor past [heightPx].
 */
internal fun threadPanelShownHeightPx(heightPx: Int, dragPx: Float, reveal: Float): Int {
    val height = heightPx.coerceAtLeast(0)
    val drag = dragPx.coerceIn(0f, height.toFloat())
    return ((height - drag) * reveal.coerceIn(0f, 1f)).roundToInt().coerceIn(0, height)
}

@Composable
private fun ChatThreadRow(
    entry: ChatThreadEntry,
    selected: Boolean,
    appearance: ChatAppearance,
    onSelect: () -> Unit,
    onDismissSelection: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onReplySwipingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val source = entry.asChatMessage()
    val message = remember(source, appearance.thirdPartyEmotes, appearance.modifierPlatforms) {
        source.withDisplayParts(appearance.thirdPartyEmotes, appearance.modifierPlatforms)
    }
    val chatTextSize = appearance.textSize
    val textSize = chatSp(chatTextSize, 13, 16)
    val timestampSize = chatSp(chatTextSize, 12, 14)
    val lineHeight = chatSp(chatTextSize, 21, 25)
    val emoteSize = chatDp(chatTextSize, 22, 26)
    val gifSize = chatDp(chatTextSize, 96, 112)
    val badgeSize = chatDp(chatTextSize, 16, 20)
    val timestampText = remember(message.timestampMillis) {
        if (message.timestampMillis > 0L) formatChatTimestamp(context, message.timestampMillis) else null
    }
    BoxMessage(
        message = message,
        selected = selected,
        timestampText = timestampText,
        appearance = appearance,
        textSize = textSize,
        timestampSize = timestampSize,
        lineHeight = lineHeight,
        emoteSize = emoteSize,
        gifSize = gifSize,
        badgeSize = badgeSize,
        onSelect = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect()
        },
        onDismissSelection = onDismissSelection,
        onReply = onReply,
        onReplySwipingChange = onReplySwipingChange,
        onCopy = {
            val text = message.copyableTextWithAuthor(timestampText.orEmpty()) ?: return@BoxMessage
            context.copyChatText(text)
            onCopy()
        },
    )
}

@Composable
private fun BoxMessage(
    message: ChatMessage,
    selected: Boolean,
    timestampText: String?,
    appearance: ChatAppearance,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
    emoteSize: Dp,
    gifSize: Dp,
    badgeSize: Dp,
    onSelect: () -> Unit,
    onDismissSelection: () -> Unit,
    onReply: () -> Unit,
    onReplySwipingChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
) {
    ChatReplySwipe(enabled = true, onReply = onReply, onSwipingChange = onReplySwipingChange) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(animatedContentAlpha(if (message.deleted) DELETED_MESSAGE_ALPHA else 1f))
            .background(animatedSelectionOverlay(selected, THREAD_SELECTED_OVERLAY_ALPHA))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClickLabel = stringResource(R.string.chat_message_actions),
                onLongClick = onSelect,
                onClick = { if (selected) onDismissSelection() },
            )
            .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
    ) {
        ChatUserMessage(
            message = message,
            readableColors = appearance.readableColors,
            timestampText = timestampText,
            textSize = textSize,
            timestampSize = timestampSize,
            lineHeight = lineHeight,
            emoteSize = emoteSize,
            gifSize = gifSize,
            badgeSize = badgeSize,
            badgeUrls = appearance.badgeUrls,
            meMessageItalic = appearance.meMessageItalic,
            onEmoteLongClick = onSelect,
        )
        if (selected) {
            ChatMessageCopyPopup(
                onReply = onReply,
                onCopy = onCopy,
                onDismiss = onDismissSelection,
            )
        }
    }
    }
}
