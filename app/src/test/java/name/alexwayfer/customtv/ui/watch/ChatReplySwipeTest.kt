package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReplySwipeTest {
    @Test
    fun `row follows the finger up to the threshold`() {
        assertEquals(0f, chatReplySwipeOffset(0f, 100f), 0.0001f)
        assertEquals(40f, chatReplySwipeOffset(40f, 100f), 0.0001f)
        assertEquals(100f, chatReplySwipeOffset(100f, 100f), 0.0001f)
    }

    @Test
    fun `travel past the threshold moves the row less than the finger`() {
        val offset = chatReplySwipeOffset(200f, 100f)
        assertTrue(offset > 100f)
        assertTrue(offset < 200f)
    }

    @Test
    fun `travel toward the end does not move the row`() {
        assertEquals(0f, chatReplySwipeOffset(-50f, 100f), 0.0001f)
    }

    @Test
    fun `release short of the threshold does not reply`() {
        assertFalse(chatReplySwipeReplies(99f, 100f))
        assertFalse(chatReplySwipeReplies(0f, 100f))
    }

    @Test
    fun `release at or past the threshold replies`() {
        assertTrue(chatReplySwipeReplies(100f, 100f))
        assertTrue(chatReplySwipeReplies(250f, 100f))
    }

    @Test
    fun `zero threshold never replies`() {
        assertFalse(chatReplySwipeReplies(10f, 0f))
    }

    @Test
    fun `arrow waits just beyond the end edge while the row is at rest`() {
        assertEquals(60f, chatReplyArrowOffset(0f, 100f, 20f), 0.0001f)
    }

    @Test
    fun `arrow slides in as the row moves and rests at the threshold`() {
        assertEquals(30f, chatReplyArrowOffset(50f, 100f, 20f), 0.0001f)
        assertEquals(0f, chatReplyArrowOffset(100f, 100f, 20f), 0.0001f)
        assertEquals(0f, chatReplyArrowOffset(130f, 100f, 20f), 0.0001f)
    }

    @Test
    fun `drag toward the end stays at rest without a dismiss action`() {
        assertEquals(0f, chatSwipeDrag(0f, -40f, canReply = true, canDismiss = false), 0.0001f)
    }

    @Test
    fun `drag toward the start stays at rest without a reply action`() {
        assertEquals(0f, chatSwipeDrag(0f, 40f, canReply = false, canDismiss = true), 0.0001f)
    }

    @Test
    fun `drag toward the end with a dismiss action follows the finger`() {
        assertEquals(-40f, chatSwipeDrag(0f, -40f, canReply = true, canDismiss = true), 0.0001f)
        assertEquals(-40f, chatSwipeOffset(-40f, 100f), 0.0001f)
        assertEquals(-250f, chatSwipeOffset(-250f, 100f), 0.0001f)
    }

    @Test
    fun `release past the threshold toward the end dismisses`() {
        assertEquals(ChatSwipeRelease.Dismiss, chatSwipeRelease(-100f, 100f))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(-99f, 100f))
    }

    @Test
    fun `release past the threshold toward the start replies`() {
        assertEquals(ChatSwipeRelease.Reply, chatSwipeRelease(100f, 100f))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(0f, 100f))
    }

    @Test
    fun `row sliding away fades out toward the end edge`() {
        assertEquals(1f, chatSwipeDismissAlpha(0f, 400), 0.0001f)
        assertEquals(1f, chatSwipeDismissAlpha(30f, 400), 0.0001f)
        assertEquals(0.5f, chatSwipeDismissAlpha(-200f, 400), 0.0001f)
        assertEquals(0f, chatSwipeDismissAlpha(-500f, 400), 0.0001f)
    }

    @Test
    fun `sideways travel within two touch slops waits`() {
        assertEquals(ChatSwipeStart.Wait, chatSwipeStart(15f, 0f, 8f))
        assertEquals(ChatSwipeStart.Wait, chatSwipeStart(-15f, 3f, 8f))
    }

    @Test
    fun `mostly sideways travel past two touch slops starts the swipe in either direction`() {
        assertEquals(ChatSwipeStart.Start, chatSwipeStart(16f, 0f, 8f))
        assertEquals(ChatSwipeStart.Start, chatSwipeStart(-16f, 7f, 8f))
        assertEquals(ChatSwipeStart.Start, chatSwipeStart(40f, -20f, 8f))
    }

    @Test
    fun `vertical travel past the touch slop before the swipe leaves the gesture to the scroll`() {
        assertEquals(ChatSwipeStart.Ignore, chatSwipeStart(0f, 8f, 8f))
        assertEquals(ChatSwipeStart.Ignore, chatSwipeStart(-20f, 12f, 8f))
    }

    @Test
    fun `diagonal travel does not start the swipe`() {
        assertEquals(ChatSwipeStart.Ignore, chatSwipeStart(30f, 16f, 8f))
    }
}
