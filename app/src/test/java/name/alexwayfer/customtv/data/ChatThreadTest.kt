package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatThreadTest {
    @Test
    fun aThreadListsTheRootThenRepliesWithoutTheLeadingMention() {
        val body = """
            {"data":{"message":{
              "id":"root","sentAt":"2024-01-01T00:00:01Z","deletedAt":null,
              "content":{"text":"hello"},
              "sender":{"login":"host","displayName":"Host"},
              "replies":{"nodes":[
                {"id":"child","sentAt":"2024-01-01T00:00:02Z","deletedAt":null,
                 "content":{"text":"@Host nice"},
                 "sender":{"login":"guest","displayName":"Guest"},
                 "parentMessage":{"id":"root"},
                 "replies":{"nodes":[]}}
              ]}
            }}}
        """.trimIndent()
        val entries = parseChatThread(body)
        assertEquals(listOf("root", "child"), entries?.map { it.id })
        assertEquals("hello", entries?.get(0)?.body)
        assertEquals("nice", entries?.get(1)?.body)
        assertEquals("Guest", entries?.get(1)?.displayName)
    }

    @Test
    fun aDeletedReplyIsLeftOut() {
        val body = """
            {"data":{"message":{
              "id":"root","sentAt":"2024-01-01T00:00:01Z",
              "content":{"text":"hello"},
              "sender":{"login":"host","displayName":"Host"},
              "replies":{"nodes":[
                {"id":"gone","sentAt":"2024-01-01T00:00:02Z","deletedAt":"2024-01-01T00:00:03Z",
                 "content":{"text":"nope"},
                 "sender":{"login":"guest","displayName":"Guest"}}
              ]}
            }}}
        """.trimIndent()
        assertEquals(listOf("root"), parseChatThread(body)?.map { it.id })
    }

    @Test
    fun aResponseWithoutAMessageDoesNotParse() {
        assertNull(parseChatThread("""{"errors":[{"message":"bad"}]}"""))
        assertNull(parseChatThread("""{"data":{"message":null}}"""))
    }
}
