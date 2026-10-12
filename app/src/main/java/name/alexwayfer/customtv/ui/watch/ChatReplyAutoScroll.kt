package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos

/**
 * Keeps the bottom of the chat in place while the selected row's reply unfolds, so the row grows upward. A list that
 * showed its end keeps its last row at the bottom; otherwise the selected row stays in sight above the bottom. The pin
 * is requested at the start of each frame, before the list lays out the new size: scrolling after the layout would
 * show the rows pushed down for a frame and then pulled back.
 */
@Composable
internal fun KeepSelectedMessageVisible(
    selected: Boolean,
    messageId: String,
    place: ChatRowPlace,
    replyFolding: () -> Boolean,
) {
    // Every row calls this; only the selected one needs the effect.
    if (!selected) return
    val listState = place.listState
    val bottomOverlayPx by rememberUpdatedState(place.bottomOverlayPx)
    LaunchedEffect(messageId) {
        var previousSize = -1
        // The row that ended the list when the selection began, while the list showed its end below the selected row.
        var endRowKey: Any? = null
        var started = false
        while (true) {
            withFrameNanos {
                val layout = listState.layoutInfo
                val item = layout.visibleItemsInfo.find { it.key == messageId }
                if (item == null) {
                    previousSize = -1
                    return@withFrameNanos
                }
                if (!started) {
                    started = true
                    val last = layout.visibleItemsInfo.last()
                    val showsEnd = chatListShowsEnd(
                        last.index,
                        last.offset + last.size,
                        layout.totalItemsCount,
                        layout.viewportEndOffset,
                    )
                    if (showsEnd && last.key != messageId) endRowKey = last.key
                }
                val endRow = endRowKey?.let { key -> layout.visibleItemsInfo.find { it.key == key } }
                val pin = chatSelectionPin(
                    SelectionFrame(
                        selected = ChatRowBox(item.index, item.offset, item.size),
                        selectedGrows = chatSelectedRowChanging(item.size, previousSize, replyFolding()),
                        anchorsEnd = endRowKey != null,
                        endRow = endRow?.let { ChatRowBox(it.index, it.offset, it.size) },
                        totalItems = layout.totalItemsCount,
                        viewportEnd = layout.viewportEndOffset,
                        bottomOverlayPx = bottomOverlayPx,
                    ),
                )
                previousSize = item.size
                if (pin != null) listState.requestScrollToItem(pin.index, pin.scrollOffset)
            }
        }
    }
}

/**
 * Whether the selected row changes size on the coming frame: its reply folds, from the frame before the first step,
 * which is large with a spring, or it changed size on the last layout.
 */
internal fun chatSelectedRowChanging(size: Int, previousSize: Int, replyFolding: Boolean): Boolean =
    replyFolding || (previousSize >= 0 && size != previousSize)

/** A row as the list last laid it out. */
internal data class ChatRowBox(val index: Int, val offset: Int, val size: Int)

/** A list position for the next layout: [index] at the top, moved up by [scrollOffset], which may be negative. */
internal data class ChatListPin(val index: Int, val scrollOffset: Int)

/**
 * One frame of the list around the selected row, as last laid out. [anchorsEnd] holds when the list showed its end
 * below the selected row as the selection began; [endRow] is the row that ended it, while it is in view.
 */
internal data class SelectionFrame(
    val selected: ChatRowBox,
    val selectedGrows: Boolean,
    val anchorsEnd: Boolean,
    val endRow: ChatRowBox?,
    val totalItems: Int,
    val viewportEnd: Int,
    val bottomOverlayPx: Int,
)

/** Whether the list shows its end: its last row is in view and ends at the visible bottom. */
internal fun chatListShowsEnd(
    lastVisibleIndex: Int,
    lastVisibleBottom: Int,
    totalItems: Int,
    viewportEnd: Int,
): Boolean = lastVisibleIndex == totalItems - 1 && lastVisibleBottom <= viewportEnd + 1

/**
 * Where to pin the list as the selected row unfolds. A list that showed its end keeps the row that ended it at the
 * bottom, so the rows under the selected one stay and it grows upward, while its top is in view; a row whose top is
 * already cut grows down in place. Otherwise the selected row stays in sight above the button over the bottom.
 */
internal fun chatSelectionPin(frame: SelectionFrame): ChatListPin? = if (frame.anchorsEnd) {
    frame.endRow?.takeIf { frame.selected.offset >= 0 }?.let { endRow ->
        chatAnchorPin(endRow, frame.viewportEnd, frame.totalItems, frame.selectedGrows)
    }
} else {
    chatAnchorPin(frame.selected, frame.viewportEnd - frame.bottomOverlayPx, frame.totalItems, frame.selectedGrows)
}

/**
 * Where to pin the list so [anchor] ends at [targetBottom] on the next layout. The row after it starts there, and the
 * list composes the rows above; the last row pins the list's end, as a row placed above the top leaves room below that
 * the list fills by scrolling back. Pins an anchor past the bottom, and an anchor at the bottom while the selected row
 * changes size; null otherwise. A pixel past the edge is rounding.
 */
internal fun chatAnchorPin(
    anchor: ChatRowBox,
    targetBottom: Int,
    totalItems: Int,
    selectedGrows: Boolean,
): ChatListPin? {
    val bottom = anchor.offset + anchor.size
    val overflows = bottom > targetBottom + 1
    val growsAtBottom = selectedGrows && bottom >= targetBottom - 1
    if (!overflows && !growsAtBottom) return null
    return if (anchor.index < totalItems - 1) {
        ChatListPin(anchor.index + 1, -targetBottom)
    } else {
        ChatListPin(anchor.index, anchor.size)
    }
}
