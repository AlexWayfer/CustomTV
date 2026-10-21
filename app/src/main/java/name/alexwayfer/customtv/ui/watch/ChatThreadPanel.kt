package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatReplySession
import name.alexwayfer.customtv.chat.ChatThreadEntry
import name.alexwayfer.customtv.chat.asChatMessage
import name.alexwayfer.customtv.chat.chatMessageCanBeMentioned
import name.alexwayfer.customtv.chat.copyableTextWithAuthor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.chat.replySessionMarksTarget
import name.alexwayfer.customtv.chat.replySessionShowsReplyingTo
import name.alexwayfer.customtv.chat.threadPanelDragCloses
import name.alexwayfer.customtv.chat.threadPinnedMaxHeightPx
import name.alexwayfer.customtv.chat.threadWindow
import name.alexwayfer.customtv.ui.theme.TwitchPurpleText
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

private const val THREAD_MOTION_MILLIS = 220
private val ThreadMotion = tween<Float>(THREAD_MOTION_MILLIS, easing = FastOutSlowInEasing)
private const val THREAD_SELECTED_OVERLAY_ALPHA = 0.24f
private const val THREAD_TARGET_OVERLAY_ALPHA = 0.14f
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
    val scope = rememberCoroutineScope()
    val onCloseState = rememberUpdatedState(onClose)
    val closeSlopPx = with(LocalDensity.current) { 72.dp.toPx() }
    val panel = remember { ThreadPanelDrag(scope, closeSlopPx, onClose = { onCloseState.value() }) }
    // Rises from the input as it opens, the way it slides back down when it closes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, ThreadMotion) }
    // Stays enabled while the panel slides away, so a second Back does not minimize the player.
    BackHandler { panel.dismiss(fromDrag = false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .threadSwipeDown(panel.offset) { reveal.value }
            .onSizeChanged { panel.panelHeight = it.height }
            .background(TwitchSurface),
    ) {
        ThreadTitleRow(starterName, panel)
        ThreadMessages(session, appearance, onChoose)
        if (replySessionShowsReplyingTo(session)) {
            ReplyingToRow(name = target?.displayName?.ifBlank { target.login }.orEmpty(), onCancel = onCancel)
        }
    }
}

/** The panel dragged down by its title, and its slide away or back when the finger lifts. */
private class ThreadPanelDrag(
    private val scope: CoroutineScope,
    private val closeSlopPx: Float,
    private val onClose: () -> Unit,
) {
    var panelHeight by mutableIntStateOf(0)
    var settling by mutableStateOf(false)
        private set
    private var drag by mutableFloatStateOf(0f)
    private val animatedOffset = Animatable(0f)
    val offset: Float get() = if (settling) animatedOffset.value else drag

    fun dragBy(dy: Float) {
        drag = (drag + dy).coerceAtLeast(0f)
    }

    /** Closes the panel; a drag too short to close it springs it back instead. */
    fun dismiss(fromDrag: Boolean) {
        if (settling) return
        val amount = drag
        settling = true
        if (fromDrag && !threadPanelDragCloses(amount, panelHeight, closeSlopPx)) {
            scope.launch {
                animatedOffset.snapTo(amount)
                animatedOffset.animateTo(0f, ThreadMotion)
                drag = 0f
                settling = false
            }
            return
        }
        scope.launch {
            animatedOffset.snapTo(amount)
            val targetOffset = panelHeight.toFloat().coerceAtLeast(1f)
            animatedOffset.animateTo(targetOffset, ThreadMotion)
            onClose()
        }
    }
}

/** The thread's title, which drags the panel down, and its close button. */
@Composable
private fun ThreadTitleRow(starterName: String, panel: ThreadPanelDrag) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy ->
                        if (!panel.settling) {
                            change.consume()
                            panel.dragBy(dy)
                        }
                    },
                    onDragCancel = { panel.dismiss(fromDrag = true) },
                    onDragEnd = { panel.dismiss(fromDrag = true) },
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
        // The button keeps its touch target without holding the low title at the 48 dp it takes.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            IconButton(onClick = { panel.dismiss(fromDrag = false) }, enabled = !panel.settling) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.chat_close_thread),
                    tint = TwitchTextSecondary,
                )
            }
        }
    }
}

/** The starter pinned on top and the replies under it, which scroll to the newest as they come. */
@Composable
private fun ColumnScope.ThreadMessages(
    session: ChatReplySession,
    appearance: ChatAppearance,
    onChoose: (String) -> Unit,
) {
    var selectedId by remember(session.starterId) { mutableStateOf<String?>(null) }
    val window = threadWindow(session.entries, session.starterId)
    val replyScroll = rememberScrollState()
    val pinnedScroll = rememberScrollState()
    // A row being swiped to reply holds the replies still, so a diagonal swipe does not scroll them.
    var replySwiping by remember { mutableStateOf(false) }
    var pinnedContentPx by remember { mutableIntStateOf(0) }
    var followingContentPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(window.following.map { it.id }) {
        snapshotFlow { replyScroll.maxValue }.collect { max -> replyScroll.animateScrollTo(max, ThreadMotion) }
    }
    val messages: @Composable (List<ChatThreadEntry>, Modifier) -> Unit = { entries, modifier ->
        ChatThreadMessages(
            entries = entries,
            selectedId = selectedId,
            marksTarget = { replySessionMarksTarget(session, it) },
            appearance = appearance,
            onSelect = { selectedId = it },
            onDismissSelection = { if (selectedId == it) selectedId = null },
            onReply = { messageId ->
                selectedId = null
                onChoose(messageId)
            },
            onCopy = { selectedId = null },
            onReplySwipingChange = { replySwiping = it },
            modifier = modifier,
        )
    }
    // Weighted, so the title and the Replying to row are measured first and stay whole when the keyboard leaves
    // little room.
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
                messages(
                    listOf(entry),
                    Modifier
                        .heightIn(max = with(LocalDensity.current) { pinnedMaxPx.toDp() })
                        .verticalScroll(pinnedScroll, enabled = !replySwiping)
                        .onSizeChanged { pinnedContentPx = it.height },
                )
            }
            if (window.following.isNotEmpty()) {
                messages(
                    window.following,
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(replyScroll, enabled = !replySwiping)
                        .onSizeChanged { followingContentPx = it.height },
                )
            }
        }
    }
}

@Composable
private fun ReplyingToRow(name: String, onCancel: () -> Unit) {
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
        // A low text button, so the row takes about a line of text rather than a Material button's 48 dp.
        Text(
            text = stringResource(R.string.cancel),
            color = TwitchPurpleText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ChatThreadMessages(
    entries: List<ChatThreadEntry>,
    selectedId: String?,
    marksTarget: (String) -> Boolean,
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
                target = marksTarget(entry.id),
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

private fun Modifier.threadSwipeDown(offsetPx: Float, reveal: () -> Float): Modifier =
    layout { measurable, constraints ->
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
    target: Boolean,
    appearance: ChatAppearance,
    onSelect: () -> Unit,
    onDismissSelection: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onReplySwipingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val message = rememberDisplayMessage(entry.asChatMessage(), appearance)
    val sizes = chatRowSizes(appearance.textSize)
    val timestampText = remember(message.timestampMillis) {
        if (message.timestampMillis > 0L) formatChatTimestamp(context, message.timestampMillis) else null
    }
    BoxMessage(
        message = message,
        selected = selected,
        target = target,
        timestampText = timestampText,
        appearance = appearance,
        sizes = sizes,
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
    target: Boolean,
    timestampText: String?,
    appearance: ChatAppearance,
    sizes: ChatRowSizes,
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
            // The message being replied to keeps a lighter mark of the same purple, under a selection's.
            .background(
                animatedSelectionOverlay(
                    selected = selected || target,
                    overlayAlpha = if (selected) THREAD_SELECTED_OVERLAY_ALPHA else THREAD_TARGET_OVERLAY_ALPHA,
                ),
            )
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
            textSize = sizes.text,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
            emoteSize = sizes.emote,
            gifSize = sizes.gif,
            badgeSize = sizes.badge,
            badgeUrls = appearance.badgeUrls,
            meMessageItalic = appearance.meMessageItalic,
            onEmoteLongClick = onSelect,
        )
        if (selected) {
            val onMention = LocalOnChatMention.current?.takeIf { chatMessageCanBeMentioned(message) }
            ChatMessageCopyPopup(
                onReply = onReply,
                onMention = onMention?.let { mention ->
                    {
                        mention(message.userLogin, message.displayName)
                        onDismissSelection()
                    }
                },
                onCopy = onCopy,
                onDismiss = onDismissSelection,
            )
        }
    }
    }
}
