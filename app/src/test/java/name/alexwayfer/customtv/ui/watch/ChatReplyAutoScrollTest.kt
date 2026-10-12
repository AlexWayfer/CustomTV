package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReplyAutoScrollTest {
    @Test
    fun `a folding reply changes the row before its first step shows`() {
        assertTrue(chatSelectedRowChanging(size = 602, previousSize = 602, replyFolding = true))
    }

    @Test
    fun `a row that changed size on the last layout keeps changing`() {
        assertTrue(chatSelectedRowChanging(size = 950, previousSize = 946, replyFolding = false))
    }

    @Test
    fun `a still row with a settled reply does not change`() {
        assertFalse(chatSelectedRowChanging(size = 950, previousSize = 950, replyFolding = false))
        assertFalse(chatSelectedRowChanging(size = 950, previousSize = -1, replyFolding = false))
    }

    @Test
    fun `a list whose last row ends at the bottom shows its end`() {
        assertTrue(chatListShowsEnd(lastVisibleIndex = 4, lastVisibleBottom = 1123, totalItems = 5, 1123))
    }

    @Test
    fun `a list with rows after the last one in view does not show its end`() {
        assertFalse(chatListShowsEnd(lastVisibleIndex = 3, lastVisibleBottom = 1123, totalItems = 5, 1123))
    }

    @Test
    fun `a list whose last row is cut by the bottom does not show its end`() {
        assertFalse(chatListShowsEnd(lastVisibleIndex = 4, lastVisibleBottom = 1200, totalItems = 5, 1123))
    }

    @Test
    fun `a row above the end grows upward and keeps the rows under it`() {
        val pin = chatSelectionPin(endFrame(selectedOffset = 422, endRow = ChatRowBox(4, 1024, 99)))
        assertEquals(ChatListPin(4, 99), pin)
    }

    @Test
    fun `the end row pushed past the bottom is pulled back`() {
        val pin = chatSelectionPin(endFrame(selectedOffset = 422, endRow = ChatRowBox(4, 1136, 99), grows = false))
        assertEquals(ChatListPin(4, 99), pin)
    }

    @Test
    fun `a row whose top is cut grows down in place`() {
        assertNull(chatSelectionPin(endFrame(selectedOffset = -180, endRow = ChatRowBox(4, 1024, 99))))
    }

    @Test
    fun `the button over the bottom does not move a list that keeps its end`() {
        val frame = endFrame(selectedOffset = 422, endRow = ChatRowBox(4, 1024, 99), grows = false)
        assertNull(chatSelectionPin(frame.copy(bottomOverlayPx = 169)))
    }

    @Test
    fun `a selected row past the bottom of a scrolled list stays above the button`() {
        val frame = SelectionFrame(
            selected = ChatRowBox(5, 850, 100),
            selectedGrows = false,
            anchorsEnd = false,
            endRow = null,
            totalItems = 10,
            viewportEnd = 1000,
            bottomOverlayPx = 100,
        )
        assertEquals(ChatListPin(6, -900), chatSelectionPin(frame))
    }

    @Test
    fun `the last row past the bottom pins the list's end`() {
        val last = ChatRowBox(index = 9, offset = 900, size = 130)
        assertEquals(ChatListPin(9, 130), chatAnchorPin(last, targetBottom = 1000, totalItems = 10, false))
    }

    @Test
    fun `an anchor at the bottom that holds still is left alone`() {
        assertNull(chatAnchorPin(ChatRowBox(4, 1024, 99), targetBottom = 1123, totalItems = 5, false))
    }

    @Test
    fun `an anchor above the bottom is left alone while the selected row grows`() {
        assertNull(chatAnchorPin(ChatRowBox(3, 500, 120), targetBottom = 1123, totalItems = 6, true))
    }

    @Test
    fun `a pixel past the bottom is rounding`() {
        assertNull(chatAnchorPin(ChatRowBox(4, 1025, 99), targetBottom = 1123, totalItems = 5, false))
    }

    private fun endFrame(selectedOffset: Int, endRow: ChatRowBox, grows: Boolean = true) = SelectionFrame(
        selected = ChatRowBox(3, selectedOffset, 602),
        selectedGrows = grows,
        anchorsEnd = true,
        endRow = endRow,
        totalItems = 5,
        viewportEnd = 1123,
        bottomOverlayPx = 0,
    )
}
