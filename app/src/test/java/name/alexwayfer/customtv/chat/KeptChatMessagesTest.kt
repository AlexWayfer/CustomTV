package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KeptChatMessagesTest {
    private val hidden = { message: ChatMessage -> message.rawText == "hidden" }

    @Test
    fun aChatWithoutHiddenMessagesKeepsTheNewestShownLimit() {
        val messages = chat(List(MAX_CHAT_MESSAGES + 30) { "shown" })
        val kept = keptChatMessages(messages, hidden)
        assertEquals(messages.takeLast(MAX_CHAT_MESSAGES), kept)
    }

    @Test
    fun hiddenMessagesBetweenTheShownOnesStayOnTop() {
        // 50 older shown, then the newest shown limit with a hidden message after each of the first 80.
        val newest = List(MAX_CHAT_MESSAGES) { index ->
            if (index < 80) listOf("shown", "hidden") else listOf("shown")
        }
        val texts = List(50) { "shown" } + newest.flatten()
        val kept = keptChatMessages(chat(texts), hidden)
        assertEquals(MAX_CHAT_MESSAGES, kept.count { !hidden(it) })
        assertEquals(MAX_CHAT_MESSAGES + 80, kept.size)
    }

    @Test
    fun aFloodOfHiddenMessagesStopsAtTheKeptLimit() {
        val texts = List(MAX_CHAT_MESSAGES) { "shown" } + List(MAX_KEPT_CHAT_MESSAGES) { "hidden" }
        val messages = chat(texts)
        val kept = keptChatMessages(messages, hidden)
        assertEquals(messages.takeLast(MAX_KEPT_CHAT_MESSAGES), kept)
    }

    @Test
    fun aShortChatIsTheSameList() {
        val messages = chat(listOf("shown", "hidden", "shown"))
        assertSame(messages, keptChatMessages(messages, hidden))
    }

    @Test
    fun anEmptyChatStaysEmpty() {
        assertEquals(emptyList<ChatMessage>(), keptChatMessages(emptyList(), hidden))
    }

    private fun chat(texts: List<String>) =
        texts.mapIndexed { index, text -> testChatMessage(id = "m$index", text = text) }
}
