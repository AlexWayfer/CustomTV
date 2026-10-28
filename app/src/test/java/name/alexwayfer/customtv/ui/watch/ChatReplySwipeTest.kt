package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.ui.MIN_TEXT_CONTRAST
import name.alexwayfer.customtv.ui.contrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReplySwipeTest {
    private val fourSteps = ChatSwipeTowardEnd.Steps(count = 4, gapPx = 50f)

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
    fun `drag toward the end stays at rest without an action there`() {
        assertEquals(0f, chatSwipeDrag(0f, -40f, canReply = true, towardEnd = ChatSwipeTowardEnd.None), 0.0001f)
    }

    @Test
    fun `drag toward the start stays at rest without a reply action`() {
        assertEquals(0f, chatSwipeDrag(0f, 40f, canReply = false, towardEnd = ChatSwipeTowardEnd.Dismiss), 0.0001f)
        assertEquals(0f, chatSwipeDrag(0f, 40f, canReply = false, towardEnd = ChatSwipeTowardEnd.Copy), 0.0001f)
    }

    @Test
    fun `drag toward the end with a dismiss action follows the finger`() {
        assertEquals(-40f, chatSwipeDrag(0f, -40f, canReply = true, towardEnd = ChatSwipeTowardEnd.Dismiss), 0.0001f)
        assertEquals(-40f, chatSwipeOffset(-40f, 100f, ChatSwipeTowardEnd.Dismiss), 0.0001f)
        assertEquals(-250f, chatSwipeOffset(-250f, 100f, ChatSwipeTowardEnd.Dismiss), 0.0001f)
    }

    @Test
    fun `drag toward the end with a copy action moves the row as a reply swipe does`() {
        assertEquals(-40f, chatSwipeDrag(0f, -40f, canReply = true, towardEnd = ChatSwipeTowardEnd.Copy), 0.0001f)
        assertEquals(-40f, chatSwipeOffset(-40f, 100f, ChatSwipeTowardEnd.Copy), 0.0001f)
        assertEquals(-chatReplySwipeOffset(250f, 100f), chatSwipeOffset(-250f, 100f, ChatSwipeTowardEnd.Copy), 0.0001f)
    }

    @Test
    fun `release past the threshold toward the end dismisses`() {
        assertEquals(ChatSwipeRelease.Dismiss, chatSwipeRelease(-100f, 100f, ChatSwipeTowardEnd.Dismiss))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(-99f, 100f, ChatSwipeTowardEnd.Dismiss))
    }

    @Test
    fun `release past the threshold toward the end copies`() {
        assertEquals(ChatSwipeRelease.Copy, chatSwipeRelease(-100f, 100f, ChatSwipeTowardEnd.Copy))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(-99f, 100f, ChatSwipeTowardEnd.Copy))
    }

    @Test
    fun `a message with text swipes to copy outside full screen`() {
        assertTrue(chatSwipeCopies(copyText = "hello", fullscreen = false))
    }

    @Test
    fun `full screen leaves the sideways swipe to the chat`() {
        assertFalse(chatSwipeCopies(copyText = "hello", fullscreen = true))
    }

    @Test
    fun `a message without text does not swipe to copy`() {
        assertFalse(chatSwipeCopies(copyText = null, fullscreen = false))
    }

    @Test
    fun `release past the threshold toward the start replies`() {
        assertEquals(ChatSwipeRelease.Reply, chatSwipeRelease(100f, 100f, ChatSwipeTowardEnd.Copy))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(0f, 100f, ChatSwipeTowardEnd.Copy))
    }

    @Test
    fun `a selected row dismisses toward the end, steps come before copying, and a row with neither stays put`() {
        assertEquals(ChatSwipeTowardEnd.Dismiss, chatSwipeTowardEnd(true, 4, copies = true, stepGapPx = 50f))
        assertEquals(fourSteps, chatSwipeTowardEnd(false, 4, copies = true, stepGapPx = 50f))
        assertEquals(ChatSwipeTowardEnd.Copy, chatSwipeTowardEnd(false, 0, copies = true, stepGapPx = 50f))
        assertEquals(ChatSwipeTowardEnd.None, chatSwipeTowardEnd(false, 0, copies = false, stepGapPx = 50f))
    }

    @Test
    fun `travel toward the end short of the threshold reaches no step`() {
        assertEquals(-1, chatSwipeStep(0f, 100f, fourSteps))
        assertEquals(-1, chatSwipeStep(99f, 100f, fourSteps))
    }

    @Test
    fun `the first step is at the threshold and each gap past it reaches the next`() {
        assertEquals(0, chatSwipeStep(100f, 100f, fourSteps))
        assertEquals(0, chatSwipeStep(149f, 100f, fourSteps))
        assertEquals(1, chatSwipeStep(150f, 100f, fourSteps))
        assertEquals(2, chatSwipeStep(200f, 100f, fourSteps))
        assertEquals(3, chatSwipeStep(250f, 100f, fourSteps))
    }

    @Test
    fun `travel past the last step stays on it`() {
        assertEquals(3, chatSwipeStep(1000f, 100f, fourSteps))
    }

    @Test
    fun `no steps or a zero threshold reach no step`() {
        assertEquals(-1, chatSwipeStep(500f, 100f, ChatSwipeTowardEnd.Steps(count = 0, gapPx = 50f)))
        assertEquals(-1, chatSwipeStep(500f, 0f, fourSteps))
    }

    @Test
    fun `a row with steps follows the finger to the last step and slows past it`() {
        assertEquals(-40f, chatSwipeDrag(0f, -40f, canReply = true, towardEnd = fourSteps), 0.0001f)
        assertEquals(-200f, chatSwipeOffset(-200f, 100f, fourSteps), 0.0001f)
        assertEquals(-250f, chatSwipeOffset(-250f, 100f, fourSteps), 0.0001f)
        assertEquals(-chatReplySwipeOffset(350f, 250f), chatSwipeOffset(-350f, 100f, fourSteps), 0.0001f)
    }

    @Test
    fun `release on a step toward the end picks the step`() {
        assertEquals(ChatSwipeRelease.Step, chatSwipeRelease(-100f, 100f, fourSteps))
        assertEquals(ChatSwipeRelease.Step, chatSwipeRelease(-400f, 100f, fourSteps))
        assertEquals(ChatSwipeRelease.Back, chatSwipeRelease(-99f, 100f, fourSteps))
        assertEquals(ChatSwipeRelease.Reply, chatSwipeRelease(100f, 100f, fourSteps))
    }

    @Test
    fun `each step reached is a stage of its own, and a single action toward either side is one`() {
        assertEquals(0, chatSwipeStage(0f, 100f, fourSteps))
        assertEquals(0, chatSwipeStage(-99f, 100f, fourSteps))
        assertEquals(1, chatSwipeStage(-100f, 100f, fourSteps))
        assertEquals(2, chatSwipeStage(-150f, 100f, fourSteps))
        assertEquals(4, chatSwipeStage(-1000f, 100f, fourSteps))
        assertEquals(1, chatSwipeStage(100f, 100f, fourSteps))
        assertEquals(1, chatSwipeStage(-100f, 100f, ChatSwipeTowardEnd.Copy))
        assertEquals(0, chatSwipeStage(-50f, 100f, ChatSwipeTowardEnd.Copy))
        assertEquals(0, chatSwipeStage(99f, 100f, ChatSwipeTowardEnd.None))
    }

    @Test
    fun `the reply and copy icons read on the colors their circles take at the threshold`() {
        listOf(ChatReplySwipeColors, ChatCopySwipeColors).forEach { colors ->
            val contrast = contrast(colors.background, colors.content)
            assertTrue("$contrast", contrast >= MIN_TEXT_CONTRAST)
        }
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
