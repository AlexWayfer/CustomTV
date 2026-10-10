package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAuthorMessagesTest {
    @Test
    fun otherAuthorsCannotEvictMessagesFromAuthorHistory() {
        val first = message("first", "alice")
        var history = rememberRecentAuthorMessages(emptyMap(), emptyList(), listOf(first))
        var visible = listOf(first)
        repeat(250) { index ->
            val next = message("other-$index", "bob")
            val updated = (visible + next).takeLast(200)
            history = rememberRecentAuthorMessages(history, visible, updated)
            visible = updated
        }
        assertEquals(listOf("first"), history["alice"]?.map(ChatMessage::id))
    }

    @Test
    fun twentyFirstMessageEvictsOnlyAuthorsOldestMessage() {
        var history = emptyMap<String, List<ChatMessage>>()
        var visible = emptyList<ChatMessage>()
        repeat(21) { index ->
            val updated = visible + message("alice-$index", "alice")
            history = rememberRecentAuthorMessages(history, visible, updated)
            visible = updated
        }
        assertEquals(20, history["alice"]?.size)
        assertEquals("alice-1", history["alice"]?.first()?.id)
        assertEquals("alice-20", history["alice"]?.last()?.id)
    }

    @Test
    fun rewardMergeUpdatesExistingMessageWithoutIncreasingCount() {
        val original = message("one", "alice")
        val enriched = original.copy(reward = ChatReward("r", "Highlight", 100), cheerBits = 10,
            reply = ChatReply("parent", "bob", "Bob", "hello"))
        val history = rememberRecentAuthorMessages(emptyMap(), emptyList(), listOf(original))
        val updated = rememberRecentAuthorMessages(history, listOf(original), listOf(enriched))
        assertEquals(listOf(enriched), updated["alice"])
    }

    @Test
    fun timeoutKeepsEvictedMessageWithDeletionState() {
        val first = message("first", "alice")
        val history = rememberRecentAuthorMessages(emptyMap(), emptyList(), listOf(first))
        val updated = markRecentAuthorMessagesDeleted(history, "alice", "mod", 45, false)
        val retained = updated["alice"]!!.single()
        assertTrue(retained.deleted)
        assertEquals(45L, retained.timeoutSeconds)
        assertEquals("mod", retained.deletedBy)
    }

    @Test
    fun deletingOneMessageKeepsItAndLeavesOtherMessagesUntouched() {
        val first = message("first", "alice")
        val second = message("second", "alice")
        val history = rememberRecentAuthorMessages(emptyMap(), emptyList(), listOf(first, second))
        val updated = markRecentAuthorMessageDeleted(history, "first", "mod")
        assertTrue(updated["alice"]!![0].deleted)
        assertFalse(updated["alice"]!![1].deleted)
    }

    @Test
    fun recentChatGoesAheadOfLiveMessagesAndKeepsTheNewest() {
        val live = (1..15).map { message("live-$it", "alice") }
        val recent = (1..10).map { message("old-$it", "alice") }
        val history = prependRecentAuthorMessages(mapOf("alice" to live), recent, emptySet())
        val ids = history["alice"]!!.map(ChatMessage::id)
        assertEquals(MAX_RECENT_AUTHOR_MESSAGES, ids.size)
        assertEquals("old-6", ids.first())
        assertEquals("live-15", ids.last())
    }

    @Test
    fun recentChatForANewAuthorStartsTheirHistory() {
        val history = prependRecentAuthorMessages(emptyMap(), listOf(message("old", "bob")), emptySet())
        assertEquals(listOf("old"), history["bob"]?.map(ChatMessage::id))
    }

    @Test
    fun movedFirstMarkLeavesTheLiveMessageInAuthorHistory() {
        val live = message("live", "alice").copy(firstInSession = true)
        val recent = message("old", "alice").copy(firstInSession = true)
        val history = prependRecentAuthorMessages(mapOf("alice" to listOf(live)), listOf(recent), setOf("alice"))
        assertEquals(listOf(true, false), history["alice"]?.map(ChatMessage::firstInSession))
    }

    private fun message(id: String, login: String) = ChatMessage(
        id = id,
        userLogin = login,
        displayName = login,
        color = Color.Unspecified,
        rawText = id,
        parts = listOf(ChatPart.Text(id)),
        timestampMillis = 0L,
    )
}
