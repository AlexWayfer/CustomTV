package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BttvEventParserTest {
    @Test
    fun parseCreate() {
        val message = emoteChangeMessage(
            """
            {"name":"emote_create","data":{
              "channel":"twitch:1",
              "emote":{
                "id":"abc",
                "code":"catJAM",
                "imageType":"gif",
                "user":{"id":"u","name":"editor","displayName":"Editor"}
              }
            }}
            """.trimIndent(),
        )
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
        val message = emoteChangeMessage(
            """{"name":"emote_delete","data":{"channel":"twitch:1","emoteId":"abc"}}""",
        )
        val change = message.emoteChange!!
        assertEquals(EmoteChangeAction.Removed, change.action)
        assertEquals("abc", change.emoteId)
        assertEquals("", change.emoteName)
        assertEquals("https://cdn.betterttv.net/emote/abc/2x.webp", change.emote?.url)
    }

    @Test
    fun parseUpdateAsRename() {
        val message = emoteChangeMessage(
            """
            {"name":"emote_update","data":{
              "channel":"twitch:1",
              "emote":{"id":"abc","code":"catJAM2","user":{"displayName":"Editor"}}
            }}
            """.trimIndent(),
        )
        val change = message.emoteChange!!
        assertEquals(EmoteChangeAction.Renamed, change.action)
        assertEquals("catJAM2", change.emoteName)
        assertEquals("abc", change.emoteId)
    }

    @Test
    fun aProChattersLookupGivesTheirPersonalEmotes() {
        val frame = BttvEventParser.parseFrame(
            """
            {"name":"lookup_user","data":{
              "name":"guntherhermann","providerId":"876901176","channel":"twitch:22484632","pro":true,
              "emotes":[
                {"id":"699b","code":"Faaah","imageType":"png","animated":false,"width":84,"height":28},
                {"id":"62b9","code":"skullSpin","imageType":"gif","animated":true}
              ]
            }}
            """.trimIndent(),
        )

        assertEquals(
            BttvFrame.Personal(
                PersonalEmoteEvent.BttvUser(
                    "876901176",
                    mapOf(
                        "Faaah" to SevenTvEmote("https://cdn.betterttv.net/emote/699b/2x.webp", 3f, id = "699b"),
                        "skullSpin" to SevenTvEmote("https://cdn.betterttv.net/emote/62b9/2x.webp", id = "62b9"),
                    ),
                ),
            ),
            frame,
        )
    }

    @Test
    fun aLookupWithoutProGivesNothing() {
        assertNull(
            BttvEventParser.parseFrame(
                """{"name":"lookup_user","data":{"providerId":"1","pro":false,"emotes":[]}}""",
            ),
        )
    }

    @Test
    fun ignoreUnknownEvents() {
        assertNull(BttvEventParser.parseFrame("""{"name":"lookup_user","data":{}}"""))
        assertNull(BttvEventParser.parseFrame("""{"type":"PING"}"""))
    }

    private fun emoteChangeMessage(text: String): ChatMessage =
        (BttvEventParser.parseFrame(text) as BttvFrame.EmoteChange).message
}
