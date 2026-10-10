package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopyableAuthorTextTest {
    @Test
    fun messageCopiesClockThenNickThenBody() {
        val message = message(displayName = "Alice", text = "hello")
        assertEquals("21:04 Alice: hello", message.copyableTextWithAuthor("21:04"))
    }

    @Test
    fun blankDisplayNameUsesLogin() {
        val message = message(displayName = "  ", text = "hello")
        assertEquals("9:05 PM alice: hello", message.copyableTextWithAuthor("9:05 PM"))
    }

    @Test
    fun replyKeepsParentMentionAfterTheAuthor() {
        val message = message(displayName = "Alice", text = "nice").copy(
            reply = ChatReply("parent", "bob", "Bob", "hi"),
        )
        assertEquals("21:04 Alice: @Bob nice", message.copyableTextWithAuthor("21:04"))
    }

    @Test
    fun noticeCopiesNothing() {
        val message = message(displayName = "Alice", text = "Welcome").copy(
            notice = ChatNotice.Welcome,
        )
        assertNull(message.copyableTextWithAuthor("21:04"))
    }

    private fun message(displayName: String, text: String) = ChatMessage(
        id = "1",
        userLogin = "alice",
        displayName = displayName,
        color = Color.Unspecified,
        rawText = text,
        parts = listOf(ChatPart.Text(text)),
        timestampMillis = 0L,
    )
}
