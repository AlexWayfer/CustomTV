package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedChatStateTest {
    @Test
    fun clearForAnotherPinIsIgnored() {
        val pin = pin()

        assertFalse(shouldClearPinnedChat(pin, "old"))
        assertTrue(shouldClearPinnedChat(pin, "current"))
        assertTrue(shouldClearPinnedChat(pin, null))
    }

    @Test
    fun durationForAnotherPinLeavesCurrentInstanceUntouched() {
        val pin = pin(endsAt = 100)

        assertSame(pin, updatePinnedChatDuration(pin, "old", 200))
    }

    @Test
    fun durationUpdateKeepsExistingEndWhenNewValueIsMissing() {
        val result = updatePinnedChatDuration(pin(100), "current", null)

        assertEquals(100L, result?.endsAtMillis)
    }

    @Test
    fun hiddenPinIsNotPublished() {
        val pin = pin()

        assertNull(visiblePinnedChat(pin, "current"))
        assertSame(pin, visiblePinnedChat(pin, "other"))
    }

    private fun pin(endsAt: Long? = null) = PinnedChat(
        pinId = "current",
        message = ChatMessage(
            id = "message",
            userLogin = "viewer",
            displayName = "Viewer",
            color = Color.White,
            rawText = "text",
            parts = listOf(ChatPart.Text("text")),
            timestampMillis = 1L,
        ),
        endsAtMillis = endsAt,
    )
}
