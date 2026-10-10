package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BttvEventParserTest {
    @Test
    fun parseCreate() {
        val message = BttvEventParser.parseFrame(
            """
            {"name":"emote_create","data":{"channel":"twitch:1","emote":{"id":"abc","code":"catJAM","imageType":"gif","user":{"id":"u","name":"editor","displayName":"Editor"}}}}
            """.trimIndent(),
        )!!
        val change = message.emoteChange!!
        assertEquals(ChatEventKind.EmoteChange, message.eventKind)
        assertEquals(EmotePlatform.Bttv, change.platform)
        assertEquals(EmoteChangeAction.Added, change.action)
        assertEquals("Editor", change.actorName)
        assertEquals("catJAM", change.emoteName)
        assertEquals("abc", change.emoteId)
        assertEquals("https://cdn.betterttv.net/emote/abc/2x.webp", change.emote?.url)
    }

    @Test
    fun parseDeleteById() {
        val message = BttvEventParser.parseFrame(
            """{"name":"emote_delete","data":{"channel":"twitch:1","emoteId":"abc"}}""",
        )!!
        val change = message.emoteChange!!
        assertEquals(EmoteChangeAction.Removed, change.action)
        assertEquals("abc", change.emoteId)
        assertEquals("", change.emoteName)
        assertEquals("https://cdn.betterttv.net/emote/abc/2x.webp", change.emote?.url)
    }

    @Test
    fun parseUpdateAsRename() {
        val message = BttvEventParser.parseFrame(
            """
            {"name":"emote_update","data":{"channel":"twitch:1","emote":{"id":"abc","code":"catJAM2","user":{"displayName":"Editor"}}}}
            """.trimIndent(),
        )!!
        val change = message.emoteChange!!
        assertEquals(EmoteChangeAction.Renamed, change.action)
        assertEquals("catJAM2", change.emoteName)
        assertEquals("abc", change.emoteId)
    }

    @Test
    fun ignoreUnknownEvents() {
        assertNull(BttvEventParser.parseFrame("""{"name":"lookup_user","data":{}}"""))
        assertNull(BttvEventParser.parseFrame("""{"type":"PING"}"""))
    }
}
