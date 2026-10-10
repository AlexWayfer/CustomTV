package name.alexwayfer.customtv.ui.watch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.data.KeywordPhrase
import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.messageMatchesKeyword
import name.alexwayfer.customtv.chat.messageMentionsUser
import name.alexwayfer.customtv.chat.copyableTextWithAuthor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.chat.smoothChatScrollMillis
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun ChatList(
    rows: List<ChatMessage>,
    recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>>,
    appearance: ChatAppearance,
    showTimestamps: Boolean,
    smoothChatScroll: Boolean,
    highlightColor: Color,
    channelColor: Color,
    highlightRewardCost: Int,
    channelPointsIconUrl: String?,
    highlightFirstMessages: Boolean,
    highlightMentions: Boolean,
    selfLogin: String,
    selfDisplayName: String,
    listState: LazyListState,
    stickToBottom: Boolean,
    onStickToBottomChange: (Boolean) -> Unit,
    onReply: ((ChatMessage) -> Unit)?,
    fieldCardOpen: OneShotRequest<ChatterCardRequest>?,
    keywordPhrases: List<KeywordPhrase>,
    linkPreviewMode: LinkPreviewMode,
    topInset: Dp,
    listTop: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    timelineJumpGeneration: Int = 0,
    /** While the full overlaid chat sinks into the compact one, turns its rows into the compact bubbles. */
    overlayRowMorph: OverlayChatRowMorph? = null,
    /** Room on both sides of the rows, as the overlaid chat keeps for the compact bubbles. */
    sideInset: Dp = 0.dp,
) {
    val messages = remember(rows) { uniqueChatRows(rows) }
    val scope = rememberCoroutineScope()
    val onStickToBottomChangeState = rememberUpdatedState(onStickToBottomChange)
    val autoScrolling = remember { AtomicBoolean(false) }
    var scrollAnchorMessageId by remember { mutableStateOf<String?>(null) }
    var animatedUntilMessageId by remember { mutableStateOf<String?>(null) }
    var catchingUp by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var missedWhilePaused by remember { mutableStateOf(false) }
    var entranceGeneration by remember { mutableIntStateOf(0) }
    var handledTimelineJumpGeneration by remember { mutableIntStateOf(0) }
    val messagesState = rememberUpdatedState(messages)
    val stickToBottomState = rememberUpdatedState(stickToBottom)
    val contentHidden = rememberUpdatedState(LocalPlayerContentHidden.current)
    var moreBelowOverlayPx by remember { mutableIntStateOf(0) }
    val moreBelowVisible = !stickToBottom && messages.isNotEmpty()
    val moreBelowGapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val bottomOverlayPx = if (moreBelowVisible && moreBelowOverlayPx > 0) {
        moreBelowOverlayPx + moreBelowGapPx
    } else {
        0
    }
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    // A row being swiped to reply holds the list still, so a diagonal swipe does not scroll the chat.
    var replySwiping by remember { mutableStateOf(false) }
    val dismissSelection = rememberUpdatedState { selectedMessageId = null }
    BackHandler(enabled = selectedMessageId != null) { selectedMessageId = null }
    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            @Suppress("SameReturnValue")
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    dismissSelection.value()
                    val wasAuto = autoScrolling.getAndSet(false)
                    if (wasAuto && available.y < 0f) entranceGeneration++
                }
                return Offset.Zero
            }

            @Suppress("SameReturnValue")
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (autoScrolling.get()) return Offset.Zero
                if (restartEntranceAfterManualScroll(
                        source == NestedScrollSource.UserInput,
                        listState.canScrollForward,
                    )
                ) entranceGeneration++
                if (!listState.canScrollBackward) {
                    scrollAnchorMessageId = null
                } else if (consumed.y != 0f) {
                    scrollAnchorMessageId = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull()
                        ?.key as? String
                }
                if (consumed.y > 0f) {
                    onStickToBottomChangeState.value(false)
                } else if (consumed.y < 0f) {
                    val lastVisibleId = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.key as? String
                    animatedUntilMessageId = entranceCursorAfterScrollingDown(
                        messagesState.value,
                        animatedUntilMessageId,
                        lastVisibleId,
                    )
                    if (!listState.canScrollForward) {
                        onStickToBottomChangeState.value(true)
                        scrollAnchorMessageId = null
                    }
                } else if (!listState.canScrollForward) {
                    onStickToBottomChangeState.value(true)
                    scrollAnchorMessageId = null
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.canScrollBackward) {
        if (!listState.canScrollBackward) scrollAnchorMessageId = null
    }

    LaunchedEffect(resumed) {
        if (!resumed) {
            if (stickToBottomState.value) missedWhilePaused = true
            return@LaunchedEffect
        }
        if (!skipEntranceAfterPause(missedWhilePaused, becameResumed = true)) return@LaunchedEffect
        missedWhilePaused = false
        if (!stickToBottomState.value || messages.isEmpty()) return@LaunchedEffect
        animatedUntilMessageId = tailAfterReturn(messages)
        catchingUp = true
        try {
            scrollToLatestMessage(listState, messages.size, autoScrolling)
        } finally {
            catchingUp = false
        }
    }

    LaunchedEffect(timelineJumpGeneration, messages.lastOrNull()?.id) {
        val current = messagesState.value
        if (timelineJumpScroll(
                generation = timelineJumpGeneration,
                handledGeneration = handledTimelineJumpGeneration,
                messageCount = current.size,
            ) != ChatReturnScroll.JumpToEnd
        ) {
            return@LaunchedEffect
        }
        handledTimelineJumpGeneration = timelineJumpGeneration
        animatedUntilMessageId = current.last().id
        catchingUp = true
        onStickToBottomChangeState.value(true)
        try {
            scrollToLatestMessage(listState, current.size, autoScrolling)
        } finally {
            catchingUp = false
        }
    }

    LaunchedEffect(stickToBottom, smoothChatScroll, catchingUp, entranceGeneration, resumed) {
        if (!stickToBottom || catchingUp || !resumed) return@LaunchedEffect
        // collect (not collectLatest): incoming messages must not cancel a row mid-animation.
        snapshotFlow { messagesState.value.lastOrNull()?.id }.collect {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            autoScrolling.set(true)
            try {
                while (autoScrolling.get() && stickToBottomState.value) {
                    val current = messagesState.value
                    if (current.isEmpty() || current.last().id == animatedUntilMessageId) break
                    // Behind the mini player nobody sees the reveal, so the list jumps.
                    val scrollMillis = if (contentHidden.value) null else smoothChatScrollMillis(smoothChatScroll)
                    val appended = appendedChatMessageCount(animatedUntilMessageId, current)
                    if (scrollMillis == null || appended == 0) {
                        listState.scrollToItem(current.lastIndex)
                        animatedUntilMessageId = current.last().id
                        break
                    }
                    val layout = listState.layoutInfo
                    val lastFullyVisibleIndex = layout.visibleItemsInfo.lastOrNull {
                        it.offset + it.size <= layout.viewportEndOffset
                    }?.index
                    val index = firstEntranceIndex(current.size - appended, lastFullyVisibleIndex)
                    if (index > current.lastIndex) {
                        animatedUntilMessageId = current.last().id
                        break
                    }
                    val revealedTail = revealChatTail(
                        listState, scrollMillis, current.size - index,
                        latestMessageId = { messagesState.value.lastOrNull()?.id },
                        shouldContinue = {
                            autoScrolling.get() && stickToBottomState.value && !contentHidden.value
                        },
                    )
                    if (!autoScrolling.get()) break
                    animatedUntilMessageId = revealedTail
                }
            } catch (_: CancellationException) {
                // A drag cancels the scroll mutation, not necessarily this effect's job.
                // Keep the observer alive after user input; propagate actual lifecycle cancellation.
                currentCoroutineContext().ensureActive()
                entranceGeneration++
            } finally {
                autoScrolling.set(false)
            }
        }
    }

    LaunchedEffect(stickToBottom, catchingUp, resumed) {
        if (!stickToBottom || catchingUp || !resumed) return@LaunchedEffect
        var pinnedOverflow = -1
        snapshotFlow {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()
            val overflow = if (last == null) {
                0
            } else {
                chatTailOverflow(
                    last.index,
                    layout.totalItemsCount,
                    last.offset + last.size,
                    layout.viewportEndOffset,
                )
            }
            val tailId = messagesState.value.lastOrNull()?.id
            TailLayout(
                overflow = overflow,
                caughtUp = tailId != null && tailId == animatedUntilMessageId,
                lastVisibleIndex = last?.index ?: -1,
                totalItems = layout.totalItemsCount,
            )
        }.collect { (overflow, caughtUp, lastVisibleIndex, totalItems) ->
            if (!stickToBottomState.value || !caughtUp) {
                pinnedOverflow = -1
                return@collect
            }
            if (chatTailPushedOutOfView(
                    stickToBottom = true,
                    scrolling = autoScrolling.get(),
                    tailCaughtUp = true,
                    lastVisibleIndex = lastVisibleIndex,
                    totalItems = totalItems,
                )
            ) {
                autoScrolling.set(true)
                try {
                    listState.scrollToItem(totalItems - 1)
                } finally {
                    autoScrolling.set(false)
                }
                pinnedOverflow = 0
                return@collect
            }
            val growth = chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = autoScrolling.get(),
                tailCaughtUp = true,
                pinnedOverflow = pinnedOverflow,
                overflow = overflow,
            )
            if (growth == 0) {
                if (!autoScrolling.get()) pinnedOverflow = overflow
                return@collect
            }
            autoScrolling.set(true)
            try {
                listState.scrollBy(growth.toFloat())
            } finally {
                autoScrolling.set(false)
            }
            pinnedOverflow = 0
        }
    }

    LaunchedEffect(messages.firstOrNull()?.id, stickToBottom, scrollAnchorMessageId) {
        val anchorId = scrollAnchorMessageId ?: return@LaunchedEffect
        if (stickToBottom || messages.any { it.id == anchorId }) return@LaunchedEffect
        animatedUntilMessageId = tailAfterReturn(messages)
        catchingUp = true
        onStickToBottomChangeState.value(true)
        try {
            scrollToLatestMessage(listState, messages.size, autoScrolling)
        } finally {
            catchingUp = false
            scrollAnchorMessageId = null
        }
    }

    val renderRow: @Composable (ChatMessage, Boolean, () -> Unit, () -> Unit, ((ChatMessage) -> Unit)?) -> Unit =
        { message, selected, onSelect, onDismissSelection, replyTo ->
            // Every shown row runs this again with each new message; the text match runs once per row.
            val mentioned = remember(message, highlightMentions, selfLogin, selfDisplayName, keywordPhrases) {
                chatRowMentioned(message, highlightMentions, selfLogin, selfDisplayName, keywordPhrases)
            }
            ChatRow(
                message,
                appearance,
                showTimestamps,
                highlightColor,
                channelColor,
                highlightRewardCost,
                channelPointsIconUrl,
                highlightFirstMessages,
                linkPreviewMode,
                mentioned = mentioned,
                listState,
                autoScrolling,
                bottomOverlayPx,
                selected = selected,
                onSelect = onSelect,
                onDismissSelection = onDismissSelection,
                onReply = replyTo,
                onReplySwipingChange = { replySwiping = it },
            )
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    ChatterCardHost(
        selfLogin = selfLogin,
        recentAuthorMessages = recentAuthorMessages,
        fieldCardOpen = fieldCardOpen,
        renderMessage = { message, replyable ->
            renderRow(
                message,
                false,
                {
                    val text = message.copyableTextWithAuthor(
                        formatChatTimestamp(context, message.timestampMillis),
                    )
                    if (text != null) context.copyChatText(text)
                },
                {},
                onReply.takeIf { replyable },
            )
        },
    ) {
    Box(
        modifier
            .keepChatAnchoredOnResize(listState, with(LocalDensity.current) { topInset.roundToPx() })
            .pointerInput(focusManager) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                            focusManager.clearFocus(force = true)
                        }
                    }
                }
            },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
            state = listState,
            userScrollEnabled = !replySwiping,
            // The notices lie over the list's top; the oldest messages scroll out from under them.
            contentPadding = PaddingValues(start = sideInset, top = topInset, end = sideInset),
            // A short chat sits at the bottom, so a notice that expands over the top does not move it.
            verticalArrangement = Arrangement.Bottom,
        ) {
            items(messages, key = { message -> message.id }) { message ->
                Box(modifier = Modifier.overlayChatRowMorph(overlayRowMorph, message.id)) {
                    renderRow(
                        message,
                        selectedMessageId == message.id,
                        { selectedMessageId = message.id },
                        { if (selectedMessageId == message.id) selectedMessageId = null },
                        onReply,
                    )
                }
            }
        }
        // Over the list, right below the notices, so it neither moves the messages nor shifts their indices.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = topInset)) {
            listTop()
        }
        AnimatedVisibility(
            visible = moreBelowVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { moreBelowOverlayPx = it.height }
                .padding(bottom = 8.dp),
            enter = fadeIn(animationSpec = tween(160)) +
                slideInVertically(animationSpec = tween(160)) { it / 2 },
            exit = fadeOut(animationSpec = tween(120)) +
                slideOutVertically(animationSpec = tween(120)) { it / 2 },
        ) {
            MoreMessagesBelowButton(
                chatTextSize = appearance.textSize,
                onClick = {
                    animatedUntilMessageId = tailAfterReturn(messages)
                    catchingUp = true
                    onStickToBottomChange(true)
                    scope.launch {
                        try {
                            scrollToLatestMessage(listState, messages.size, autoScrolling)
                        } finally {
                            catchingUp = false
                        }
                    }
                },
            )
        }
    }
    }
}

/** Whether a row shows as a mention: it names the signed-in user, or matches one of their keywords. */
internal fun chatRowMentioned(
    message: ChatMessage,
    highlightMentions: Boolean,
    selfLogin: String,
    selfDisplayName: String,
    keywordPhrases: List<KeywordPhrase>,
): Boolean =
    (highlightMentions && messageMentionsUser(message, selfLogin, selfDisplayName)) ||
        messageMatchesKeyword(message, keywordPhrases, selfLogin)

private data class TailLayout(
    val overflow: Int,
    val caughtUp: Boolean,
    val lastVisibleIndex: Int,
    val totalItems: Int,
)

private suspend fun scrollToLatestMessage(
    listState: LazyListState,
    messageCount: Int,
    autoScrolling: AtomicBoolean,
) {
    val lastIndex = messageCount - 1
    if (chatReturnScroll(messageCount) != ChatReturnScroll.JumpToEnd || lastIndex < 0) return
    autoScrolling.set(true)
    try {
        listState.scrollToItem(lastIndex)
    } finally {
        autoScrolling.set(false)
    }
}
