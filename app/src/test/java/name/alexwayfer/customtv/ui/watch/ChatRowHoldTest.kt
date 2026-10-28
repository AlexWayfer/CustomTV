package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRowHoldTest {
    @Test
    fun `a stopped list keeps its top row in place`() {
        assertEquals("top", chatRowHoldAnchor(canScrollBackward = true, firstVisibleMessageId = "top"))
    }

    @Test
    fun `a list at its oldest row keeps no anchor`() {
        assertNull(chatRowHoldAnchor(canScrollBackward = false, firstVisibleMessageId = "top"))
    }

    @Test
    fun `a reply keeps the chat still until the message is sent`() {
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Reply, rowsBelow = false))
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Reply, rowsBelow = true))
    }

    @Test
    fun `a mention keeps the chat still until the message is sent`() {
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Mention, rowsBelow = false))
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Mention, rowsBelow = true))
    }

    @Test
    fun `a copy returns to the newest message`() {
        assertTrue(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Copy, rowsBelow = true))
    }

    @Test
    fun `a dismissal with nothing new below follows again`() {
        assertTrue(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Dismiss, rowsBelow = false))
    }

    @Test
    fun `a dismissal with new rows below leaves the chat for the button`() {
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = true, ChatRowHoldEnd.Dismiss, rowsBelow = true))
    }

    @Test
    fun `a copy on a chat that already stood still returns to the newest message`() {
        assertTrue(chatFollowsAfterRowHold(stoppedFollowing = false, ChatRowHoldEnd.Copy, rowsBelow = true))
    }

    @Test
    fun `a dismissal on a chat that already stood still does not start following`() {
        assertFalse(chatFollowsAfterRowHold(stoppedFollowing = false, ChatRowHoldEnd.Dismiss, rowsBelow = false))
    }

    @Test
    fun `a swipe ends the hold as the menu action it matches`() {
        assertEquals(ChatRowHoldEnd.Reply, chatRowHoldEndOfSwipe(ChatSwipeRelease.Reply))
        assertEquals(ChatRowHoldEnd.Copy, chatRowHoldEndOfSwipe(ChatSwipeRelease.Copy))
    }

    @Test
    fun `a swipe that springs back or dismisses ends the hold as a dismissal`() {
        assertEquals(ChatRowHoldEnd.Dismiss, chatRowHoldEndOfSwipe(ChatSwipeRelease.Back))
        assertEquals(ChatRowHoldEnd.Dismiss, chatRowHoldEndOfSwipe(ChatSwipeRelease.Dismiss))
    }

    @Test
    fun `a swipe released on a step ends the hold as a dismissal, as a menu action that asks in a dialog does`() {
        assertEquals(ChatRowHoldEnd.Dismiss, chatRowHoldEndOfSwipe(ChatSwipeRelease.Step))
    }

    @Test
    fun `the hold ends once no swipe or menu holds a row`() {
        assertTrue(chatRowHoldEnds(swiping = false, selectedMessageId = null))
    }

    @Test
    fun `a swipe that ends while a menu is open keeps the hold`() {
        assertFalse(chatRowHoldEnds(swiping = false, selectedMessageId = "menu"))
    }

    @Test
    fun `a menu closed during a swipe keeps the hold`() {
        assertFalse(chatRowHoldEnds(swiping = true, selectedMessageId = null))
    }
}
