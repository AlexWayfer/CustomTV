package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAppendAnimationTest {
    @Test
    fun manualArrivalAtBottomRestartsObserverEvenIfAlreadyFollowing() {
        assertEquals(true, restartEntranceAfterManualScroll(userInput = true, canScrollForward = false))
        assertEquals(false, restartEntranceAfterManualScroll(userInput = true, canScrollForward = true))
        assertEquals(false, restartEntranceAfterManualScroll(userInput = false, canScrollForward = false))
    }

    @Test
    fun countsMessagesAppendedAfterThePreviousTail() {
        assertEquals(0, appendedChatMessageCount(null, listOf(message("one"))))
        assertEquals(1, appendedChatMessageCount("one", listOf(message("one"), message("two"))))
        // Still one new message when an old message was trimmed at the limit.
        assertEquals(1, appendedChatMessageCount("two", listOf(message("two"), message("three"))))
        assertEquals(2, appendedChatMessageCount("one", listOf(message("one"), message("two"), message("three"))))
        assertEquals(0, appendedChatMessageCount("one", listOf(message("two"), message("three"))))
        assertEquals(0, appendedChatMessageCount("one", listOf(message("one"))))
    }

    @Test
    fun returningToTheBottomDoesNotEntranceAnimateTheBuffer() {
        val buffer = listOf(message("one"), message("two"), message("three"))
        val tail = tailAfterReturn(buffer)

        assertEquals("three", tail)
        assertEquals(0, appendedChatMessageCount(tail, buffer))
        assertEquals(1, appendedChatMessageCount(tail, buffer + message("four")))
    }

    @Test
    fun pauseSkipsEntranceAnimationOnlyWhenTheChatWasStuckToTheBottom() {
        assertEquals(true, skipEntranceAfterPause(wasStuckToBottom = true, becameResumed = true))
        assertEquals(false, skipEntranceAfterPause(wasStuckToBottom = false, becameResumed = true))
        assertEquals(false, skipEntranceAfterPause(wasStuckToBottom = true, becameResumed = false))
    }

    @Test
    fun scrollingDownDoesNotPullTheEntranceCursorBackward() {
        val messages = listOf(message("one"), message("two"), message("three"), message("four"))

        assertEquals(
            "three",
            entranceCursorAfterScrollingDown(messages, previousTailMessageId = "one", lastVisibleMessageId = "three"),
        )
        assertEquals(
            "three",
            entranceCursorAfterScrollingDown(messages, previousTailMessageId = "three", lastVisibleMessageId = "two"),
        )
        assertEquals("two", entranceCursorAfterScrollingDown(messages, previousTailMessageId = null, lastVisibleMessageId = "two"))
        assertEquals(6, firstEntranceIndex(firstNewIndex = 2, lastFullyVisibleIndex = 5))
        assertEquals(2, firstEntranceIndex(firstNewIndex = 2, lastFullyVisibleIndex = 1))
        assertEquals(2, firstEntranceIndex(firstNewIndex = 2, lastFullyVisibleIndex = null))
    }

    @Test
    fun partiallyVisibleFirstRowOfBatchIsNotSkipped() {
        // Row 2 has entered the viewport, but only row 1 is completely visible.
        assertEquals(2, firstEntranceIndex(firstNewIndex = 2, lastFullyVisibleIndex = 1))
        assertEquals(3, firstEntranceIndex(firstNewIndex = 2, lastFullyVisibleIndex = 2))
    }

    @Test
    fun arrivalsDuringBatchAndBufferTrimmingKeepTheNextPendingRow() {
        var buffer = listOf(message("one"), message("two"), message("three"))
        var cursor = "one"
        val revealed = mutableListOf<String>()
        repeat(3) {
            val next = buffer.size - appendedChatMessageCount(cursor, buffer)
            cursor = buffer[next].id
            revealed += cursor
            if (cursor == "two") {
                buffer = listOf(message("two"), message("three"), message("four"))
            }
        }
        assertEquals(listOf("two", "three", "four"), revealed)
        assertEquals(0, appendedChatMessageCount(cursor, buffer))
    }

    @Test
    fun returnScrollJumpsEvenWhenSmoothScrollIsOn() {
        assertEquals(ChatReturnScroll.JumpToEnd, chatReturnScroll(messageCount = 3))
        assertEquals(ChatReturnScroll.Stay, chatReturnScroll(messageCount = 0))
    }

    @Test
    fun timelineSeekJumpsDirectlyToLatestReplayMessage() {
        assertEquals(ChatReturnScroll.Stay, timelineJumpScroll(generation = 0, handledGeneration = 0, messageCount = 20_000))
        assertEquals(ChatReturnScroll.Stay, timelineJumpScroll(generation = 1, handledGeneration = 0, messageCount = 0))
        assertEquals(ChatReturnScroll.JumpToEnd, timelineJumpScroll(generation = 1, handledGeneration = 0, messageCount = 20_000))
        assertEquals(ChatReturnScroll.Stay, timelineJumpScroll(generation = 1, handledGeneration = 1, messageCount = 20_001))
    }

    private fun message(id: String) = ChatMessage(
        id = id,
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = id,
        parts = emptyList(),
        timestampMillis = 0L,
    )
}
