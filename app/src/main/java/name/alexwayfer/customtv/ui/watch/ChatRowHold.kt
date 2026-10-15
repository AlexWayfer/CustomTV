package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * How the viewer let go of a held row: replied to it, mentioned its chatter, copied it, or closed the menu or the swipe
 * without any of them.
 */
internal enum class ChatRowHoldEnd { Reply, Mention, Copy, Dismiss }

/**
 * A row the viewer holds: swiped to reply, or selected with a long press for its menu. Holding stops a following chat,
 * as a scroll up does, so new rows do not slide the row from under the finger; a swipe also holds the list's own
 * scroll still, so a diagonal swipe does not scroll the chat. [onAnchorChange] sets the row the list keeps in place.
 */
@Stable
internal class ChatRowHold(
    private val stickToBottom: State<Boolean>,
    private val onStickToBottomChange: State<(Boolean) -> Unit>,
    private val onAnchorChange: (String?) -> Unit,
) {
    var swiping by mutableStateOf(false)
        private set
    var selectedMessageId by mutableStateOf<String?>(null)
        private set

    /**
     * A finger rests on the list. New rows wait until it lifts, so a row does not slide away before a long press or a
     * swipe takes it; the chat keeps following, so a tap does not bring up the button.
     */
    var touching by mutableStateOf(false)
        private set
    private var stoppedFollowing = false
    private var replied = false

    fun onSwipingChange(swiping: Boolean, listState: LazyListState) {
        this.swiping = swiping
        if (swiping) hold(listState)
    }

    fun onSwipeEnd(release: ChatSwipeRelease, listState: LazyListState) {
        release(if (release == ChatSwipeRelease.Reply) ChatRowHoldEnd.Reply else ChatRowHoldEnd.Dismiss, listState)
    }

    fun select(messageId: String, listState: LazyListState) {
        selectedMessageId = messageId
        hold(listState)
    }

    fun dismissSelection(end: ChatRowHoldEnd, listState: LazyListState) {
        if (selectedMessageId == null) return
        selectedMessageId = null
        release(end, listState)
    }

    /** A scroll by the viewer closes the menu and leaves following to the scroll. */
    fun dropSelection() {
        if (selectedMessageId == null) return
        selectedMessageId = null
        if (swiping) return
        stoppedFollowing = false
        replied = false
    }

    /** Follows each touch on the list from the first finger down to the last one up, without taking it. */
    suspend fun trackTouches(input: PointerInputScope) {
        input.awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            touching = true
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                touching = false
            }
        }
    }

    private fun hold(listState: LazyListState) {
        if (!stickToBottom.value) return
        stoppedFollowing = true
        onAnchorChange(
            chatRowHoldAnchor(
                listState.canScrollBackward,
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key as? String,
            ),
        )
        onStickToBottomChange.value(false)
    }

    private fun release(end: ChatRowHoldEnd, listState: LazyListState) {
        replied = replied || end == ChatRowHoldEnd.Reply
        if (!chatRowHoldEnds(swiping, selectedMessageId)) return
        val follows = chatFollowsAfterRowHold(
            stoppedFollowing,
            if (replied) ChatRowHoldEnd.Reply else end,
            listState.hasRowsBelow(),
        )
        stoppedFollowing = false
        replied = false
        if (!follows) return
        onAnchorChange(null)
        onStickToBottomChange.value(true)
    }
}

/** The hold ends once neither a swipe nor an open menu keeps a row held. */
internal fun chatRowHoldEnds(swiping: Boolean, selectedMessageId: String?): Boolean =
    !swiping && selectedMessageId == null

/**
 * The row a list that a held row stopped keeps in place: its top row, as after a scroll up. None while the list sits
 * at its oldest row.
 */
internal fun chatRowHoldAnchor(canScrollBackward: Boolean, firstVisibleMessageId: String?): String? =
    firstVisibleMessageId.takeIf { canScrollBackward }

/**
 * Whether the chat follows again once a hold ends. A copy always returns to the newest message, even when an earlier
 * hold or a scroll had already stopped the chat. A reply or a mention keeps it still until the message is sent. A
 * dismissal of a hold that stopped the chat keeps it still when rows came below meanwhile, so the viewer returns with
 * the button, and follows again otherwise, since nothing below would move.
 */
internal fun chatFollowsAfterRowHold(stoppedFollowing: Boolean, end: ChatRowHoldEnd, rowsBelow: Boolean): Boolean =
    when (end) {
        ChatRowHoldEnd.Reply, ChatRowHoldEnd.Mention -> false
        ChatRowHoldEnd.Copy -> true
        ChatRowHoldEnd.Dismiss -> stoppedFollowing && !rowsBelow
    }
