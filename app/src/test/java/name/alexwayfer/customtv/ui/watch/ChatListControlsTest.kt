package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListControlsTest {
    @Test
    fun `a row after the last one in view lies below`() {
        assertTrue(chatRowsBelow(lastVisibleIndex = 8, totalItems = 10))
    }

    @Test
    fun `the last row cut by the bottom edge is not a row below`() {
        assertFalse(chatRowsBelow(lastVisibleIndex = 9, totalItems = 10))
    }

    @Test
    fun `an empty list has no rows below`() {
        assertFalse(chatRowsBelow(lastVisibleIndex = null, totalItems = 0))
    }

    @Test
    fun `a chat that stays put with rows below shows the button`() {
        assertTrue(moreMessagesBelowVisible(stickToBottom = false) { true })
    }

    @Test
    fun `a chat stopped by a held row with nothing below hides the button`() {
        assertFalse(moreMessagesBelowVisible(stickToBottom = false) { false })
    }

    @Test
    fun `a following chat hides the button without reading the rows below`() {
        assertFalse(moreMessagesBelowVisible(stickToBottom = true) { error("read rows below") })
    }
}
