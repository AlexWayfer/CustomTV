package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.smoothChatScrollMillis
import name.alexwayfer.customtv.ui.rememberLifecycleResumed
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun rememberChatListScroll(
    messages: List<ChatMessage>,
    stickToBottom: Boolean,
    onStickToBottomChange: (Boolean) -> Unit,
): ChatListScroll {
    val messagesState = rememberUpdatedState(messages)
    val stickToBottomState = rememberUpdatedState(stickToBottom)
    val onStickToBottomChangeState = rememberUpdatedState(onStickToBottomChange)
    val contentHidden = rememberUpdatedState(LocalPlayerContentHidden.current)
    return remember {
        ChatListScroll(messagesState, stickToBottomState, onStickToBottomChangeState, contentHidden)
    }
}

/** Keeps the chat at its newest message while it follows the chat, and slides new rows in. */
@Composable
internal fun ChatListScrollEffects(
    scroll: ChatListScroll,
    listState: LazyListState,
    messages: List<ChatMessage>,
    smoothChatScroll: Boolean,
    timelineJumpGeneration: Int,
) {
    val resumed = rememberLifecycleResumed()
    val following = scroll.following
    LaunchedEffect(listState.canScrollBackward) {
        if (!listState.canScrollBackward) scroll.scrollAnchorMessageId = null
    }
    LaunchedEffect(resumed) { scroll.onResumedChange(resumed, listState, messages) }
    LaunchedEffect(timelineJumpGeneration, messages.lastOrNull()?.id) {
        scroll.jumpAfterTimelineJump(timelineJumpGeneration, listState)
    }
    val scrollsToNewRows = chatScrollsToNewRows(following, scroll.catchingUp, resumed, scroll.rowHold.touching)
    LaunchedEffect(scrollsToNewRows, smoothChatScroll, scroll.entranceGeneration) {
        if (!scrollsToNewRows) return@LaunchedEffect
        scroll.revealNewMessages(listState, smoothChatScroll)
    }
    LaunchedEffect(scrollsToNewRows) {
        if (!scrollsToNewRows) return@LaunchedEffect
        scroll.followTailGrowth(listState)
    }
    LaunchedEffect(messages.firstOrNull()?.id, following, scroll.scrollAnchorMessageId) {
        scroll.returnWhenAnchorDropped(listState, messages, following)
    }
}

/**
 * The chat list's scroll: whether it follows new messages, the rows already slid in, the row the viewer scrolled to,
 * and the row the viewer holds. The [messages], [stickToBottom], and callbacks are the latest the list was composed
 * with.
 */
@Stable
internal class ChatListScroll(
    private val messages: State<List<ChatMessage>>,
    private val stickToBottom: State<Boolean>,
    private val onStickToBottomChange: State<(Boolean) -> Unit>,
    private val contentHidden: State<Boolean>,
) {
    val autoScrolling = AtomicBoolean(false)
    var scrollAnchorMessageId by mutableStateOf<String?>(null)
    private var animatedUntilMessageId by mutableStateOf<String?>(null)
    var catchingUp by mutableStateOf(false)
        private set
    private var missedWhilePaused by mutableStateOf(false)
    var entranceGeneration by mutableIntStateOf(0)
        private set
    private var handledTimelineJumpGeneration by mutableIntStateOf(0)
    val rowHold = ChatRowHold(stickToBottom, onStickToBottomChange) { scrollAnchorMessageId = it }

    val following: Boolean get() = stickToBottom.value

    fun nestedScrollConnection(listState: LazyListState): NestedScrollConnection = object : NestedScrollConnection {
        @Suppress("SameReturnValue")
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y != 0f) {
                rowHold.dropSelection()
                val wasAuto = autoScrolling.getAndSet(false)
                if (wasAuto && available.y < 0f) entranceGeneration++
            }
            return Offset.Zero
        }

        @Suppress("SameReturnValue")
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
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
                onStickToBottomChange.value(false)
            } else if (consumed.y < 0f) {
                val lastVisibleId = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.key as? String
                animatedUntilMessageId = entranceCursorAfterScrollingDown(
                    messages.value,
                    animatedUntilMessageId,
                    lastVisibleId,
                )
                if (!listState.canScrollForward) {
                    onStickToBottomChange.value(true)
                    scrollAnchorMessageId = null
                }
            } else if (!listState.canScrollForward) {
                onStickToBottomChange.value(true)
                scrollAnchorMessageId = null
            }
            return Offset.Zero
        }
    }

    /** Starts a jump to the newest message: rows up to [cursor] count as slid in; with [follow] the chat follows. */
    private fun startCatchUp(cursor: String?, follow: Boolean) {
        animatedUntilMessageId = cursor
        catchingUp = true
        if (follow) onStickToBottomChange.value(true)
    }

    private suspend fun finishCatchUp(listState: LazyListState, messageCount: Int) {
        try {
            scrollToLatestMessage(listState, messageCount, autoScrolling)
        } finally {
            catchingUp = false
        }
    }

    /** More messages below: the chat jumps to the newest one and follows again. */
    fun returnToLatest(scope: CoroutineScope, listState: LazyListState, messages: List<ChatMessage>) {
        startCatchUp(tailAfterReturn(messages), follow = true)
        scope.launch { finishCatchUp(listState, messages.size) }
    }

    suspend fun onResumedChange(resumed: Boolean, listState: LazyListState, messages: List<ChatMessage>) {
        if (!resumed) {
            if (stickToBottom.value) missedWhilePaused = true
            return
        }
        if (!skipEntranceAfterPause(missedWhilePaused, becameResumed = true)) return
        missedWhilePaused = false
        if (!stickToBottom.value || messages.isEmpty()) return
        startCatchUp(tailAfterReturn(messages), follow = false)
        finishCatchUp(listState, messages.size)
    }

    suspend fun jumpAfterTimelineJump(generation: Int, listState: LazyListState) {
        val current = messages.value
        val scroll = timelineJumpScroll(
            generation = generation,
            handledGeneration = handledTimelineJumpGeneration,
            messageCount = current.size,
        )
        if (scroll != ChatReturnScroll.JumpToEnd) return
        handledTimelineJumpGeneration = generation
        startCatchUp(current.last().id, follow = true)
        finishCatchUp(listState, current.size)
    }

    /** A list scrolled up to a row that left the capped list goes back to the newest message. */
    suspend fun returnWhenAnchorDropped(listState: LazyListState, messages: List<ChatMessage>, following: Boolean) {
        val anchorId = scrollAnchorMessageId ?: return
        if (following || messages.any { it.id == anchorId }) return
        startCatchUp(tailAfterReturn(messages), follow = true)
        try {
            finishCatchUp(listState, messages.size)
        } finally {
            scrollAnchorMessageId = null
        }
    }

    /** Slides each new message in as it comes, while the chat follows. */
    suspend fun revealNewMessages(listState: LazyListState, smoothChatScroll: Boolean) {
        // collect (not collectLatest): incoming messages must not cancel a row mid-animation.
        snapshotFlow { messages.value.lastOrNull()?.id }.collect { _ ->
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            autoScrolling.set(true)
            try {
                var revealing = true
                while (revealing && autoScrolling.get() && stickToBottom.value) {
                    revealing = revealNewRows(listState, smoothChatScroll)
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

    /** One step toward the newest row; false when it is shown or the list jumped to it. */
    private suspend fun revealNewRows(listState: LazyListState, smoothChatScroll: Boolean): Boolean {
        val current = messages.value
        // Behind the mini player nobody sees the reveal, so the list jumps.
        val scrollMillis = if (contentHidden.value) null else smoothChatScrollMillis(smoothChatScroll)
        val appended = appendedChatMessageCount(animatedUntilMessageId, current)
        return when {
            current.isEmpty() || current.last().id == animatedUntilMessageId -> false
            scrollMillis == null || appended == 0 -> {
                listState.scrollToItem(current.lastIndex)
                animatedUntilMessageId = current.last().id
                false
            }
            else -> revealAppended(listState, current, appended, scrollMillis)
        }
    }

    /** Slides the rows that arrived after [appended] in; false when the next rows cannot follow on their own. */
    private suspend fun revealAppended(
        listState: LazyListState,
        current: List<ChatMessage>,
        appended: Int,
        scrollMillis: Int,
    ): Boolean {
        val layout = listState.layoutInfo
        val lastFullyVisibleIndex = layout.visibleItemsInfo.lastOrNull {
            it.offset + it.size <= layout.viewportEndOffset
        }?.index
        val index = firstEntranceIndex(current.size - appended, lastFullyVisibleIndex)
        if (index > current.lastIndex) {
            animatedUntilMessageId = current.last().id
            return false
        }
        val revealedTail = revealChatTail(
            listState, scrollMillis, current.size - index,
            latestMessageId = { messages.value.lastOrNull()?.id },
            shouldContinue = {
                autoScrolling.get() && stickToBottom.value && !contentHidden.value
            },
        )
        val stillScrolling = autoScrolling.get()
        if (stillScrolling) animatedUntilMessageId = revealedTail
        return stillScrolling
    }

    /** Keeps the newest row in sight while it grows, as when an emote or a preview loads in it. */
    suspend fun followTailGrowth(listState: LazyListState) {
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
            val tailId = messages.value.lastOrNull()?.id
            TailLayout(
                overflow = overflow,
                caughtUp = tailId != null && tailId == animatedUntilMessageId,
                lastVisibleIndex = last?.index ?: -1,
                totalItems = layout.totalItemsCount,
            )
        }.collect { tail ->
            if (!stickToBottom.value || !tail.caughtUp) {
                pinnedOverflow = -1
                return@collect
            }
            if (chatTailPushedOutOfView(
                    stickToBottom = true,
                    scrolling = autoScrolling.get(),
                    tailCaughtUp = true,
                    lastVisibleIndex = tail.lastVisibleIndex,
                    totalItems = tail.totalItems,
                )
            ) {
                autoScrolling.whileSet { listState.scrollToItem(tail.totalItems - 1) }
                pinnedOverflow = 0
                return@collect
            }
            val growth = chatTailGrowthToFollow(
                stickToBottom = true,
                scrolling = autoScrolling.get(),
                tailCaughtUp = true,
                pinnedOverflow = pinnedOverflow,
                overflow = tail.overflow,
            )
            if (growth == 0) {
                if (!autoScrolling.get()) pinnedOverflow = tail.overflow
                return@collect
            }
            autoScrolling.whileSet { listState.scrollBy(growth.toFloat()) }
            pinnedOverflow = 0
        }
    }
}

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
    autoScrolling.whileSet { listState.scrollToItem(lastIndex) }
}

/** Marks the list as scrolling on its own while [scroll] runs, so the scroll reads as not the viewer's. */
private suspend fun AtomicBoolean.whileSet(scroll: suspend () -> Unit) {
    set(true)
    try {
        scroll()
    } finally {
        set(false)
    }
}
