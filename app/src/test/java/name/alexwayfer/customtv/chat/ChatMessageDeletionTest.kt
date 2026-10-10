package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageDeletionTest {
    @Test
    fun marksOnlyMessageWithMatchingId() {
        val first = message("first", "viewer")
        val second = message("second", "viewer")

        val result = markMessageDeleted(listOf(first, second), "second", "moderator")

        assertFalse(result[0].deleted)
        assertTrue(result[1].deleted)
        assertEquals("moderator", result[1].deletedBy)
    }

    @Test
    fun keepsExistingModeratorWhenEventDoesNotProvideOne() {
        val deleted = message("first", "viewer").copy(deleted = true, deletedBy = "original")

        val result = markMessageDeleted(listOf(deleted), "first", null)

        assertEquals("original", result.single().deletedBy)
    }

    @Test
    fun marksUsersMessagesButNotSystemNotices() {
        val viewerMessage = message("first", "viewer")
        val otherMessage = message("second", "other")
        val notice = chatNoticeMessage(ChatNotice.Welcome, "notice").copy(userLogin = "viewer")

        val result = markUserMessagesDeleted(
            listOf(viewerMessage, otherMessage, notice),
            "viewer",
            "moderator",
        )

        assertTrue(result[0].deleted)
        assertFalse(result[1].deleted)
        assertFalse(result[2].deleted)
    }

    @Test
    fun blankLoginLeavesOriginalListUntouched() {
        val messages = listOf(message("first", "viewer"))

        val result = markUserMessagesDeleted(messages, "", "moderator")

        assertSame(messages, result)
    }

    @Test
    fun propagatesTimeoutAndBanToDeletedMessages() {
        val messages = listOf(message("first", "viewer"), message("second", "other"))

        val timedOut = markUserMessagesDeleted(messages, "viewer", null, timeoutSeconds = 45)
        assertEquals(45L, timedOut[0].timeoutSeconds)
        assertFalse(timedOut[0].banned)
        assertNull(timedOut[1].timeoutSeconds)

        val banned = markUserMessagesDeleted(timedOut, "viewer", null, banned = true)
        assertNull(banned[0].timeoutSeconds)
        assertTrue(banned[0].banned)
    }

    @Test
    fun formatsShortAndLongTimeouts() {
        assertEquals("0:45", formatTimeoutDuration(45))
        assertEquals("10:00", formatTimeoutDuration(600))
        assertEquals("1:02:03", formatTimeoutDuration(3_723))
        assertEquals("14d", formatTimeoutDuration(14 * 86_400L))
        assertEquals("2d 3h", formatTimeoutDuration(2 * 86_400L + 3 * 3_600L))
    }

    private fun message(id: String, login: String) = ChatMessage(
        id = id,
        userLogin = login,
        displayName = login,
        color = Color.White,
        rawText = "text",
        parts = listOf(ChatPart.Text("text")),
        timestampMillis = 1L,
    )
}
