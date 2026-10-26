package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SevenTvEventParserTest {
    @Test
    fun parseHelloHeartbeat() {
        val frame = SevenTvEventParser.parseFrame(
            """{"op":1,"d":{"heartbeat_interval":25000,"session_id":"abc"}}""",
        )
        assertTrue(frame is SevenTvFrame.Hello)
        assertEquals(25_000L, (frame as SevenTvFrame.Hello).heartbeatMs)
    }

    @Test
    fun parseAddedEmoteRendersHostUrl() {
        val frame = SevenTvEventParser.parseFrame(dispatch(pushed = emoteChange()))
        val messages = (frame as SevenTvFrame.Dispatch).messages
        assertEquals(1, messages.size)
        val message = messages[0]
        assertEquals(ChatEventKind.EmoteChange, message.eventKind)
        val change = message.emoteChange!!
        assertEquals(EmotePlatform.SevenTv, change.platform)
        assertEquals(EmoteChangeAction.Added, change.action)
        assertEquals("AlexWayfer", change.actorName)
        assertEquals(Color.Unspecified, change.actorColor)
        assertEquals("Sadge", change.emoteName)
        assertEquals("https://cdn.7tv.app/emote/sadge-id/2x.webp", change.emote?.url)
        assertEquals("sadge-id", change.emote?.id)
        assertEquals(1_700_000_000_000L, message.timestampMillis)
    }

    @Test
    fun parseRemovedAndRenamedEmotes() {
        val json = JSONObject()
            .put("op", 0)
            .put("t", 1_700_000_000L)
            .put(
                "d",
                JSONObject()
                    .put("type", "emote_set.update")
                    .put(
                        "body",
                        JSONObject()
                            .put("actor", JSONObject().put("display_name", "Editor"))
                            .put(
                                "pulled",
                                JSONArray().put(
                                    JSONObject().put("old_value", emoteJson("Oldge", "old-id")),
                                ),
                            )
                            .put(
                                "updated",
                                JSONArray().put(
                                    JSONObject()
                                        .put("old_value", emoteJson("peepoHappy", "happy-id"))
                                        .put("value", emoteJson("widepeepoHappy", "happy-id")),
                                ),
                            ),
                    ),
            )
            .toString()
        val messages = (SevenTvEventParser.parseFrame(json) as SevenTvFrame.Dispatch).messages
        assertEquals(2, messages.size)
        val removed = messages[0].emoteChange!!
        assertEquals(EmoteChangeAction.Removed, removed.action)
        assertEquals("Oldge", removed.emoteName)
        val renamed = messages[1].emoteChange!!
        assertEquals(EmoteChangeAction.Renamed, renamed.action)
        assertEquals("peepoHappy", renamed.previousName)
        assertEquals("widepeepoHappy", renamed.emoteName)
        assertEquals(1_700_000_000_000L, messages[0].timestampMillis)
    }

    @Test
    fun ignoreNonEmoteSetDispatch() {
        val frame = SevenTvEventParser.parseFrame(
            """{"op":0,"d":{"type":"cosmetic.create","body":{}}}""",
        )
        assertTrue(frame is SevenTvFrame.Dispatch)
        assertTrue((frame as SevenTvFrame.Dispatch).messages.isEmpty())
    }

    @Test
    fun dispatchWithoutNestedBodyStillParsesPushed() {
        val json = JSONObject()
            .put("op", 0)
            .put(
                "d",
                JSONObject()
                    .put("type", "emote_set.update")
                    .put("actor", JSONObject().put("display_name", "Editor"))
                    .put("pushed", JSONArray().put(emoteChange())),
            )
            .toString()
        val messages = (SevenTvEventParser.parseFrame(json) as SevenTvFrame.Dispatch).messages
        assertEquals(1, messages.size)
        assertEquals("Sadge", messages[0].emoteChange?.emoteName)
    }

    @Test
    fun theChannelSetsUpdateStaysAChatLine() {
        val frame = SevenTvEventParser.parseFrame(dispatch(pushed = emoteChange(), setId = "channel"), "channel")

        assertEquals("Sadge", (frame as SevenTvFrame.Dispatch).messages.single().emoteChange?.emoteName)
    }

    @Test
    fun anotherSetsUpdateChangesThatPersonalSet() {
        val renamed = JSONObject()
            .put("old_value", emoteJson("peepoHappy", "happy-id"))
            .put("value", emoteJson("widepeepoHappy", "happy-id"))
        val json = JSONObject()
            .put("op", 0)
            .put(
                "d",
                JSONObject()
                    .put("type", "emote_set.update")
                    .put(
                        "body",
                        JSONObject()
                            .put("id", "personal")
                            .put("pushed", JSONArray().put(emoteChange()))
                            .put("pulled", JSONArray().put(JSONObject().put("old_value", emoteJson("Oldge", "old"))))
                            .put("updated", JSONArray().put(renamed)),
                    ),
            )
            .toString()

        val frame = SevenTvEventParser.parseFrame(json, "channel")

        val change = (frame as SevenTvFrame.Personal).event as PersonalEmoteEvent.SevenTvSetChanged
        assertEquals("personal", change.setId)
        assertEquals(setOf("Sadge", "widepeepoHappy"), change.added.keys)
        assertEquals("https://cdn.7tv.app/emote/sadge-id/2x.webp", change.added["Sadge"]?.url)
        assertEquals(setOf("Oldge", "peepoHappy"), change.removed)
    }

    @Test
    fun anEmoteSetEntitlementGivesTheSetToTheChattersTwitchAccount() {
        val frame = SevenTvEventParser.parseFrame(
            """
            {"op":0,"d":{"type":"entitlement.create","body":{"id":"0","kind":5,"object":{
              "id":"0","kind":"EMOTE_SET","ref_id":"personal",
              "user":{"id":"7tv-user","username":"infected0660","connections":[
                {"id":"557969778053218304","platform":"DISCORD"},
                {"id":"431995282","platform":"TWITCH","username":"infected0660"}
              ]}
            }}}}
            """.trimIndent(),
            "channel",
        )

        assertEquals(
            SevenTvFrame.Personal(PersonalEmoteEvent.SevenTvSetAssigned("personal", listOf("431995282"))),
            frame,
        )
    }

    @Test
    fun otherEntitlementsLeaveNoTrace() {
        val avatar = SevenTvEventParser.parseFrame(
            """
            {"op":0,"d":{"type":"entitlement.create","body":{"object":{
              "kind":"AVATAR","ref_id":"avatar","user":{"connections":[{"id":"1","platform":"TWITCH"}]}
            }}}}
            """.trimIndent(),
            "channel",
        )
        val noTwitch = SevenTvEventParser.parseFrame(
            """
            {"op":0,"d":{"type":"entitlement.create","body":{"object":{
              "kind":"EMOTE_SET","ref_id":"set","user":{"connections":[{"id":"1","platform":"KICK"}]}
            }}}}
            """.trimIndent(),
            "channel",
        )

        assertEquals(SevenTvFrame.Dispatch(emptyList()), avatar)
        assertEquals(SevenTvFrame.Dispatch(emptyList()), noTwitch)
    }

    private fun dispatch(pushed: JSONObject, setId: String? = null): String {
        return JSONObject()
            .put("op", 0)
            .put("t", 1_700_000_000_000L)
            .put(
                "d",
                JSONObject()
                    .put("type", "emote_set.update")
                    .put(
                        "body",
                        JSONObject()
                            .putOpt("id", setId)
                            .put(
                                "actor",
                                JSONObject()
                                    .put("display_name", "AlexWayfer")
                                    .put("style", JSONObject().put("color", 4_278_190_335L)),
                            )
                            .put("pushed", JSONArray().put(pushed)),
                    ),
            )
            .toString()
    }

    private fun emoteChange(): JSONObject {
        return JSONObject().put("value", emoteJson("Sadge", "sadge-id"))
    }

    private fun emoteJson(name: String, id: String): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put(
                "data",
                JSONObject()
                    .put("id", id)
                    .put("name", name)
                    .put("flags", 0)
                    .put(
                        "host",
                        JSONObject()
                            .put("url", "//cdn.7tv.app/emote/$id")
                            .put(
                                "files",
                                JSONArray().put(
                                    JSONObject()
                                        .put("name", "2x.webp")
                                        .put("width", 64)
                                        .put("height", 64)
                                        .put("format", "WEBP"),
                                ),
                            ),
                    ),
            )
    }
}
