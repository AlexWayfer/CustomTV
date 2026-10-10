package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatReplyComposerTest {
    @Test
    fun `signed in with confirmed rules opens the keyboard`() {
        assertEquals(
            ChatReplyComposer.Keyboard,
            chatReplyComposer(canSend = true, warned = false, rulesNeedConfirmation = false),
        )
    }

    @Test
    fun `signed in with unconfirmed rules opens the rules first`() {
        assertEquals(
            ChatReplyComposer.Rules,
            chatReplyComposer(canSend = true, warned = false, rulesNeedConfirmation = true),
        )
    }

    @Test
    fun `logged out opens only the thread`() {
        assertEquals(
            ChatReplyComposer.Nothing,
            chatReplyComposer(canSend = false, warned = false, rulesNeedConfirmation = false),
        )
        assertEquals(
            ChatReplyComposer.Nothing,
            chatReplyComposer(canSend = false, warned = false, rulesNeedConfirmation = true),
        )
    }

    @Test
    fun `a warning to acknowledge opens only the thread, rules or not`() {
        assertEquals(
            ChatReplyComposer.Nothing,
            chatReplyComposer(canSend = true, warned = true, rulesNeedConfirmation = false),
        )
        assertEquals(
            ChatReplyComposer.Nothing,
            chatReplyComposer(canSend = true, warned = true, rulesNeedConfirmation = true),
        )
    }
}
