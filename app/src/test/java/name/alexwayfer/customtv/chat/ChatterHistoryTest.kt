package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterHistoryTest {
    private val nobody = ChatMentionSelf()

    @Test
    fun replyMentionsTheAuthorItAnswers() {
        val reply = message("r", "alice", "sure").copy(reply = ChatReply("parent", "Bobby", "Bobby", "hi"))
        assertEquals(setOf("bobby"), mentionedLogins(reply, emptySet(), nobody))
    }

    @Test
    fun atLoginMentionsAChatterWhoHasNotWrittenYet() {
        assertEquals(setOf("carol"), mentionedLogins(message("m", "alice", "hi @Carol"), emptySet(), nobody))
    }

    @Test
    fun bareLoginMentionsOnlyAKnownChatter() {
        val message = message("m", "alice", "dave and erin are here")
        assertEquals(setOf("dave"), mentionedLogins(message, setOf("dave"), nobody))
    }

    @Test
    fun authorDoesNotMentionThemself() {
        assertEquals(emptySet<String>(), mentionedLogins(message("m", "alice", "@alice me"), setOf("alice"), nobody))
    }

    @Test
    fun selfDisplayNameMentionsTheSignedInUser() {
        val self = ChatMentionSelf(login = "viewer", displayName = "Зритель")
        assertEquals(setOf("viewer"), mentionedLogins(message("m", "alice", "привет, Зритель"), emptySet(), self))
    }

    @Test
    fun noticeMentionsNobody() {
        val notice = chatNoticeMessage(ChatNotice.Connecting, "n").copy(rawText = "@alice")
        assertEquals(emptySet<String>(), mentionedLogins(notice, emptySet(), nobody))
    }

    @Test
    fun mentionGoesToEachNamedChatterAndNotToItsAuthor() {
        val mention = message("m", "alice", "@bobby @carol look")
        val history = rememberChatterHistory(ChatterHistory(), emptyList(), listOf(mention), nobody)
        assertEquals(listOf("m"), history.mentions["bobby"]?.map(ChatMessage::id))
        assertEquals(listOf("m"), history.mentions["carol"]?.map(ChatMessage::id))
        assertNull(history.mentions["alice"])
        assertEquals(listOf("m"), history.messages["alice"]?.map(ChatMessage::id))
    }

    @Test
    fun twentyFirstMentionEvictsTheOldest() {
        var history = ChatterHistory()
        var visible = emptyList<ChatMessage>()
        repeat(MAX_RECENT_MENTIONS + 1) { index ->
            val updated = visible + message("m-$index", "alice", "@bobby $index")
            history = rememberChatterHistory(history, visible, updated, nobody)
            visible = updated
        }
        val ids = history.mentions.getValue("bobby").map(ChatMessage::id)
        assertEquals(MAX_RECENT_MENTIONS, ids.size)
        assertEquals("m-1", ids.first())
        assertEquals("m-$MAX_RECENT_MENTIONS", ids.last())
    }

    @Test
    fun updatedMessageReplacesItsMentionWithoutAddingOne() {
        val original = message("m", "alice", "@bobby hi")
        val enriched = original.copy(cheerBits = 10)
        val history = rememberChatterHistory(ChatterHistory(), emptyList(), listOf(original), nobody)
        val updated = rememberChatterHistory(history, listOf(original), listOf(enriched), nobody)
        assertEquals(listOf(enriched), updated.mentions["bobby"])
    }

    @Test
    fun deletingAMessageMarksItsMention() {
        val history = rememberChatterHistory(
            ChatterHistory(),
            emptyList(),
            listOf(message("m", "alice", "@bobby hi"), message("n", "carol", "@bobby yo")),
            nobody,
        )
        val mentions = markChatterMessageDeleted(history, "m", "mod").mentions.getValue("bobby")
        assertTrue(mentions[0].deleted)
        assertFalse(mentions[1].deleted)
    }

    @Test
    fun timeoutMarksTheAuthorsMentionsOfOthers() {
        val mentioning = listOf(message("m", "alice", "@bobby hi"))
        val history = rememberChatterHistory(ChatterHistory(), emptyList(), mentioning, nobody)
        val marked = markChatterMessagesDeleted(history, "Alice", "mod", 30, false)
        val mention = marked.mentions.getValue("bobby").single()
        assertTrue(mention.deleted)
        assertEquals(30L, mention.timeoutSeconds)
    }

    @Test
    fun recentChatMentionsGoAheadOfLiveOnes() {
        val liveMessages = listOf(message("live", "alice", "@bobby"))
        val live = rememberChatterHistory(ChatterHistory(), emptyList(), liveMessages, nobody)
        val history = prependChatterHistory(live, listOf(message("old", "carol", "@bobby")), emptySet(), nobody)
        assertEquals(listOf("old", "live"), history.mentions["bobby"]?.map(ChatMessage::id))
    }

    private fun message(id: String, login: String, text: String) =
        testChatMessage(id, text, login = login, color = Color.Unspecified, timestampMillis = 0L)
}
