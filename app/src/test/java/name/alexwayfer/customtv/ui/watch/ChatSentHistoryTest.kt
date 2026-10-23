package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatEventKind
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.ChatReply
import name.alexwayfer.customtv.chat.ChatterHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSentHistoryTest {
    private fun message(
        id: String,
        text: String,
        login: String = "Me",
        reply: ChatReply? = null,
        isAction: Boolean = false,
        systemText: String? = null,
        eventKind: ChatEventKind = ChatEventKind.Normal,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = id,
        userLogin = login,
        displayName = login,
        color = Color.White,
        rawText = text,
        parts = listOf(ChatPart.Text(text)),
        timestampMillis = 0L,
        reply = reply,
        isAction = isAction,
        systemText = systemText,
        eventKind = eventKind,
        deleted = deleted,
        deletedBy = if (deleted) "mod" else null,
    )

    private fun sent(id: String, text: String) = ChatSentMessage(message(id, text), text)

    private fun List<ChatSentMessage>.texts() = map { it.id to it.text }

    private val streamerReply = ChatReply(
        parentMsgId = "p",
        parentUserLogin = "streamer",
        parentDisplayName = "Streamer",
        parentBody = "",
    )

    private val one = sent("1", "first")
    private val two = sent("2", "second")
    private val three = sent("3", "third")
    private val history = listOf(one, two, three)

    @Test
    fun `the history is the user's own messages, oldest first, whatever the login's case`() {
        val chatters = ChatterHistory(
            messages = mapOf(
                "me" to listOf(message("1", "first"), message("2", "second")),
                "other" to listOf(message("3", "not mine", login = "other")),
            ),
        )

        assertEquals(listOf("1" to "first", "2" to "second"), chatSentHistory(chatters, selfLogin = "ME").texts())
    }

    @Test
    fun `no login has no history`() {
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(message("1", "first"))))

        assertEquals(emptyList<ChatSentMessage>(), chatSentHistory(chatters, selfLogin = null))
        assertEquals(emptyList<ChatSentMessage>(), chatSentHistory(chatters, selfLogin = " "))
    }

    @Test
    fun `a reply comes back with the nick it answered, as a copy of it reads`() {
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(message("1", "hi there", reply = streamerReply))))

        assertEquals(listOf("1" to "@Streamer hi there"), chatSentHistory(chatters, "me").texts())
    }

    @Test
    fun `a reply shows the nick it answered ahead of its text and emotes`() {
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(message("1", "hi there", reply = streamerReply))))

        val shown = chatSentHistory(chatters, "me").single().message
        assertEquals(listOf(ChatPart.Text("@Streamer "), ChatPart.Text("hi there")), shown.parts)
    }

    @Test
    fun `a reply that already starts with the nick shows as it is`() {
        val original = message("1", "@streamer hi there", reply = streamerReply)
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(original)))

        assertEquals(original.parts, chatSentHistory(chatters, "me").single().message.parts)
    }

    @Test
    fun `a deleted message shows as the user wrote it, without the deletion`() {
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(message("1", "oops", deleted = true))))

        val shown = chatSentHistory(chatters, "me").single().message
        assertFalse(shown.deleted)
        assertNull(shown.deletedBy)
    }

    @Test
    fun `a me message comes back as the command`() {
        val chatters = ChatterHistory(messages = mapOf("me" to listOf(message("1", "waves", isAction = true))))

        assertEquals(listOf("1" to "/me waves"), chatSentHistory(chatters, "me").texts())
    }

    @Test
    fun `a subscription without the user's own words is left out`() {
        val chatters = ChatterHistory(
            messages = mapOf(
                "me" to listOf(
                    message("1", "", systemText = "subscribed for 5 months", eventKind = ChatEventKind.Subscription),
                    message("2", "thanks for the stream", eventKind = ChatEventKind.Subscription),
                ),
            ),
        )

        assertEquals(listOf("2" to "thanks for the stream"), chatSentHistory(chatters, "me").texts())
    }

    @Test
    fun `the plaque opens on the newest message`() {
        assertEquals(three, chatHistoryShown(history, shownId = null))
    }

    @Test
    fun `the plaque falls back to the newest message once the shown one has left the history`() {
        assertEquals(three, chatHistoryShown(history, shownId = "gone"))
    }

    @Test
    fun `an empty history shows nothing`() {
        assertNull(chatHistoryShown(emptyList(), shownId = null))
    }

    @Test
    fun `the plaque pages to the older and the newer message and stops at the ends`() {
        assertEquals(two, chatHistoryStep(history, shownId = "3", older = true))
        assertEquals(one, chatHistoryStep(history, shownId = "2", older = true))
        assertEquals(one, chatHistoryStep(history, shownId = "1", older = true))
        assertEquals(two, chatHistoryStep(history, shownId = "1", older = false))
        assertEquals(three, chatHistoryStep(history, shownId = "3", older = false))
    }

    @Test
    fun `the plaque can page only where a message is left`() {
        assertTrue(chatHistoryCanStep(history, shownId = "3", older = true))
        assertFalse(chatHistoryCanStep(history, shownId = "3", older = false))
        assertFalse(chatHistoryCanStep(history, shownId = "1", older = true))
        assertTrue(chatHistoryCanStep(history, shownId = "1", older = false))
        assertFalse(chatHistoryCanStep(listOf(one), shownId = "1", older = true))
    }

    @Test
    fun `up on a draft puts the newest message in the field and keeps the draft`() {
        val result = chatHistoryKey(history, ChatHistoryRecall(), fieldText = "half typed", older = true)

        assertEquals(ChatHistoryKeyResult(ChatHistoryRecall(shownId = "3", draft = "half typed"), "third"), result)
    }

    @Test
    fun `up goes on to older messages and stops at the oldest`() {
        val atNewest = ChatHistoryRecall(shownId = "3", draft = "")
        val atOldest = ChatHistoryRecall(shownId = "1", draft = "")

        assertEquals(
            ChatHistoryKeyResult(ChatHistoryRecall(shownId = "2", draft = ""), "second"),
            chatHistoryKey(history, atNewest, fieldText = "third", older = true),
        )
        assertNull(chatHistoryKey(history, atOldest, fieldText = "first", older = true))
    }

    @Test
    fun `down goes back to newer messages and past the newest brings the draft back`() {
        val atOldest = ChatHistoryRecall(shownId = "1", draft = "half typed")
        val atNewest = ChatHistoryRecall(shownId = "3", draft = "half typed")

        assertEquals(
            ChatHistoryKeyResult(ChatHistoryRecall(shownId = "2", draft = "half typed"), "second"),
            chatHistoryKey(history, atOldest, fieldText = "first", older = false),
        )
        assertEquals(
            ChatHistoryKeyResult(ChatHistoryRecall(), "half typed"),
            chatHistoryKey(history, atNewest, fieldText = "third", older = false),
        )
    }

    @Test
    fun `down on a draft leaves the key to the field`() {
        assertNull(chatHistoryKey(history, ChatHistoryRecall(), fieldText = "half typed", older = false))
    }

    @Test
    fun `an edited message becomes the draft, and up starts again from the newest`() {
        val recalled = ChatHistoryRecall(shownId = "2", draft = "old draft")

        assertEquals(
            ChatHistoryKeyResult(ChatHistoryRecall(shownId = "3", draft = "second, edited"), "third"),
            chatHistoryKey(history, recalled, fieldText = "second, edited", older = true),
        )
        assertNull(chatHistoryKey(history, recalled, fieldText = "second, edited", older = false))
    }

    @Test
    fun `a recalled message that left the history counts as a draft`() {
        val recalled = ChatHistoryRecall(shownId = "gone", draft = "old draft")

        assertEquals(
            ChatHistoryKeyResult(ChatHistoryRecall(shownId = "3", draft = "gone text"), "third"),
            chatHistoryKey(history, recalled, fieldText = "gone text", older = true),
        )
    }

    @Test
    fun `without a history the keys stay with the field`() {
        assertNull(chatHistoryKey(emptyList(), ChatHistoryRecall(), fieldText = "", older = true))
    }

    @Test
    fun `the history button shows while the composer is open and there is something to recall`() {
        assertTrue(chatHistoryButtonShown(enabled = true, canSend = true, composerOpen = true, hasHistory = true))
        assertFalse(chatHistoryButtonShown(enabled = true, canSend = true, composerOpen = false, hasHistory = true))
        assertFalse(chatHistoryButtonShown(enabled = true, canSend = true, composerOpen = true, hasHistory = false))
        assertFalse(chatHistoryButtonShown(enabled = false, canSend = true, composerOpen = true, hasHistory = true))
        assertFalse(chatHistoryButtonShown(enabled = true, canSend = false, composerOpen = true, hasHistory = true))
    }

    @Test
    fun `a swipe down pages to the older message and up to the newer one, as the chat above runs`() {
        assertEquals(true, chatHistorySwipeOlder(dx = 3f, dy = 40f))
        assertEquals(false, chatHistorySwipeOlder(dx = -3f, dy = -40f))
    }

    @Test
    fun `a sideways or still swipe pages nothing`() {
        assertNull(chatHistorySwipeOlder(dx = 40f, dy = 10f))
        assertNull(chatHistorySwipeOlder(dx = -40f, dy = -10f))
        assertNull(chatHistorySwipeOlder(dx = 0f, dy = 0f))
    }

    @Test
    fun `completions take the plaque's place while they show`() {
        assertTrue(chatHistoryPlaqueShown(open = true, buttonShown = true, completionShown = false))
        assertFalse(chatHistoryPlaqueShown(open = true, buttonShown = true, completionShown = true))
        assertFalse(chatHistoryPlaqueShown(open = false, buttonShown = true, completionShown = false))
        assertFalse(chatHistoryPlaqueShown(open = true, buttonShown = false, completionShown = false))
    }
}
