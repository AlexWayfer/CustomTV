package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRaidersTest {
    private val raider = ChatRaider(sourceChannelId = "1", sourceDisplayName = "AlexWayfer")

    @Test
    fun markIsKeptByMessageId() {
        assertEquals(mapOf("m1" to raider), chatRaidersAfter(emptyMap(), "m1", raider))
    }

    @Test
    fun blankMessageIdIsIgnored() {
        assertEquals(emptyMap<String, ChatRaider>(), chatRaidersAfter(emptyMap(), " ", raider))
    }

    @Test
    fun onlyTheNewestMarksOfOneChatScreenAreKept() {
        var marks = emptyMap<String, ChatRaider>()
        for (index in 0..MAX_CHAT_MESSAGES) marks = chatRaidersAfter(marks, "m$index", raider)
        assertEquals(MAX_CHAT_MESSAGES, marks.size)
        assertEquals("m1", marks.keys.first())
        assertEquals("m$MAX_CHAT_MESSAGES", marks.keys.last())
    }
}
