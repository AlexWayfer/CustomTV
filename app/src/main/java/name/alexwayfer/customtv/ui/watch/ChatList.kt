package name.alexwayfer.customtv.ui.watch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.copyableTextWithAuthor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.chat.messageMatchesKeyword
import name.alexwayfer.customtv.chat.messageMentionsUser
import name.alexwayfer.customtv.data.KeywordPhrase

@Composable
internal fun ChatList(
    feed: ChatFeed,
    settings: ChatListSettings,
    layout: ChatListLayout,
    listTop: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = feed.messages()
    val messages = remember(rows) { uniqueChatRows(rows) }
    val listState = feed.listState
    val scope = rememberCoroutineScope()
    val scroll = rememberChatListScroll(messages, feed.stickToBottom, feed.onStickToBottomChange)
    BackHandler(enabled = scroll.rowHold.selectedMessageId != null) {
        scroll.rowHold.dismissSelection(ChatRowHoldEnd.Dismiss, listState)
    }
    ChatListScrollEffects(scroll, listState, messages, settings.smoothScroll, feed.timelineJumpGeneration)
    var moreBelowOverlayPx by remember { mutableIntStateOf(0) }
    // Derived, so a scrolled-up list recomposes only when the answer changes, not on every scrolled frame.
    val rowsBelow by remember(listState) { derivedStateOf { listState.hasRowsBelow() } }
    val moreBelowVisible = moreMessagesBelowVisible(feed.stickToBottom) { rowsBelow }
    val moreBelowGapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val bottomOverlayPx = if (moreBelowVisible && moreBelowOverlayPx > 0) moreBelowOverlayPx + moreBelowGapPx else 0
    val rowLook = ChatListRowLook(
        settings = settings,
        place = ChatRowPlace(listState, bottomOverlayPx),
        overlayRowMorph = layout.overlayRowMorph,
        onReplySwipingChange = { scroll.rowHold.onSwipingChange(it, listState) },
        onReplySwipeEnd = { scroll.rowHold.onSwipeEnd(it, listState) },
    )
    val context = LocalContext.current
    ChatterCardHost(
        selfLogin = settings.mentions.selfLogin,
        chatterHistory = feed.chatterHistory,
        fieldCardOpen = feed.fieldCardOpen,
        renderMessage = { message, replyable ->
            val copy = { copyChatMessage(context, message) }
            rowLook.Message(message, selected = false, copy, onDismissSelection = {}, feed.onReply.takeIf { replyable })
        },
    ) {
        Box(
            modifier
                .keepChatAnchoredOnResize(listState, with(LocalDensity.current) { layout.topInset.roundToPx() })
                .clearFocusOnTouch(LocalFocusManager.current),
        ) {
            ChatListMessages(
                messages = messages,
                rowLook = rowLook,
                scroll = scroll,
                onReply = feed.onReply,
                // The notices lie over the list's top; the oldest messages scroll out from under them.
                contentPadding = PaddingValues(start = layout.sideInset, top = layout.topInset, end = layout.sideInset),
            )
            // Over the list, right below the notices, so it neither moves the messages nor shifts their indices.
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = layout.topInset)) {
                listTop()
            }
            MoreMessagesBelow(
                visible = moreBelowVisible,
                chatTextSize = settings.appearance.textSize,
                onHeightChange = { moreBelowOverlayPx = it },
                onClick = { scroll.returnToLatest(scope, listState, messages) },
            )
        }
    }
}

/** Where the list sits: under the notices that lie over its top, and inside an overlaid chat. */
internal data class ChatListLayout(
    val topInset: Dp,
    /** Room on both sides of the rows, as the overlaid chat keeps for the compact bubbles. */
    val sideInset: Dp = 0.dp,
    /** While the full overlaid chat sinks into the compact one, turns its rows into the compact bubbles. */
    val overlayRowMorph: OverlayChatRowMorph? = null,
)

/** How the list draws each row, the same for every row of the list. */
internal data class ChatListRowLook(
    val settings: ChatListSettings,
    val place: ChatRowPlace,
    val overlayRowMorph: OverlayChatRowMorph?,
    val onReplySwipingChange: (Boolean) -> Unit,
    val onReplySwipeEnd: (ChatSwipeRelease) -> Unit,
) {
    @Composable
    fun Message(
        message: ChatMessage,
        selected: Boolean,
        onSelect: () -> Unit,
        onDismissSelection: (ChatRowHoldEnd) -> Unit,
        onReply: ((ChatMessage) -> Unit)?,
    ) {
        ChatRow(
            message = message,
            settings = settings,
            place = place,
            selected = selected,
            actions = ChatRowActions(onSelect, onDismissSelection, onReply, onReplySwipingChange, onReplySwipeEnd),
        )
    }
}

@Composable
private fun ChatListMessages(
    messages: List<ChatMessage>,
    rowLook: ChatListRowLook,
    scroll: ChatListScroll,
    onReply: ((ChatMessage) -> Unit)?,
    contentPadding: PaddingValues,
) {
    val listState = rowLook.place.listState
    val nestedScrollConnection = remember(listState) { scroll.nestedScrollConnection(listState) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(scroll.rowHold) { scroll.rowHold.trackTouches(this) },
        state = listState,
        userScrollEnabled = !scroll.rowHold.swiping,
        contentPadding = contentPadding,
        // A short chat sits at the bottom, so a notice that expands over the top does not move it.
        verticalArrangement = Arrangement.Bottom,
    ) {
        items(messages, key = { message -> message.id }) { message ->
            Box(modifier = Modifier.overlayChatRowMorph(rowLook.overlayRowMorph, message.id)) {
                rowLook.Message(
                    message,
                    scroll.rowHold.selectedMessageId == message.id,
                    { scroll.rowHold.select(message.id, listState) },
                    { end ->
                        if (scroll.rowHold.selectedMessageId == message.id) {
                            scroll.rowHold.dismissSelection(end, listState)
                        }
                    },
                    onReply,
                )
            }
        }
    }
}

/** The button that returns a scrolled-up chat to its newest message; [onHeightChange] reports the room it takes. */
@Composable
private fun BoxScope.MoreMessagesBelow(
    visible: Boolean,
    chatTextSize: Int,
    onHeightChange: (Int) -> Unit,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .onSizeChanged { onHeightChange(it.height) }
            .padding(bottom = 8.dp),
        enter = MoreMessagesBelowEnter,
        exit = MoreMessagesBelowExit,
    ) {
        MoreMessagesBelowButton(chatTextSize = chatTextSize, onClick = onClick)
    }
}

private fun copyChatMessage(context: Context, message: ChatMessage) {
    val text = message.copyableTextWithAuthor(formatChatTimestamp(context, message.timestampMillis))
    if (text != null) context.copyChatText(text)
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
