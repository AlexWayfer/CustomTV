package name.alexwayfer.customtv.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelPointsPubSubParserTest {
    @Test
    fun parsePostureCheckRedemptionWithoutUserInput() {
        val inner = """
            {"type":"reward-redeemed","data":{"timestamp":"2026-09-17T14:00:00.000000000Z","redemption":{"id":"redemption-1","user":{"id":"117474239","login":"alexwayfer","display_name":"AlexWayfer"},"channel_id":"117474239","redeemed_at":"2026-09-17T14:00:00.000000000Z","reward":{"id":"4eb8ee0f-2c05-4848-9ed4-d369b3ff5986","title":"Posture Check!","cost":100,"background_color":"#019B00","image":{"url_1x":"https://example.com/custom-1.png","url_2x":"https://example.com/custom-2.png"},"default_image":{"url_1x":"https://static-cdn.jtvnw.net/custom-reward-images/clock-1.png"}},"status":"UNFULFILLED"}}}
        """.trimIndent()
        val frame = JSONObject()
            .put("type", "MESSAGE")
            .put(
                "data",
                JSONObject()
                    .put("topic", "community-points-channel-v1.117474239")
                    .put("message", inner),
            )
            .toString()
        val message = ChannelPointsPubSubParser.parseFrame(frame)
        assertNotNull(message)
        assertEquals(ChatEventKind.Reward, message!!.eventKind)
        assertEquals("AlexWayfer", message.displayName)
        assertEquals("alexwayfer", message.userLogin)
        assertEquals("117474239", message.userId)
        assertEquals("Posture Check!", message.reward?.title)
        assertEquals(100, message.reward?.cost)
        assertEquals("#019B00", message.reward?.backgroundColorHex)
        assertEquals("https://example.com/custom-2.png", message.reward?.imageUrl)
        assertTrue(message.parts.isEmpty())
        assertEquals("reward-redemption-1", message.id)
        assertEquals(0, message.rawText.length)
    }

    @Test
    fun parseRedemptionWithUserInput() {
        val inner = """
            {"type":"reward-redeemed","data":{"redemption":{"id":"redemption-2","user":{"login":"viewer","display_name":"Viewer"},"reward":{"id":"song","title":"Request a song","cost":1000},"user_input":"Never Gonna Give You Up"}}}
        """.trimIndent()
        val message = ChannelPointsPubSubParser.parseRedemptionMessage(inner)
        assertNotNull(message)
        assertNull(message!!.userId)
        assertEquals("Never Gonna Give You Up", message.rawText)
        assertEquals("Never Gonna Give You Up", (message.parts[0] as ChatPart.Text).text)
        assertEquals("Request a song", message.reward?.title)
        assertFalse(message.reward!!.giantEmote)
    }

    @Test
    fun parseGiantEmoteRewardFromPrompt() {
        val inner = """
            {"type":"reward-redeemed","data":{"redemption":{"id":"redemption-3","user":{"login":"alexwayfer","display_name":"AlexWayfer"},"reward":{"id":"giants","title":"АТАКА ГИГАНТОВ","prompt":"Напиши эмоут. FFZ:GE","cost":10000},"user_input":"coin"}}}
        """.trimIndent()
        val message = ChannelPointsPubSubParser.parseRedemptionMessage(inner)
        assertNotNull(message)
        assertEquals("coin", message!!.rawText)
        assertEquals("Напиши эмоут. FFZ:GE", message.reward?.prompt)
        assertTrue(message.reward!!.giantEmote)
    }

    @Test
    fun ignoreNonRedemptionFrames() {
        assertNull(ChannelPointsPubSubParser.parseFrame("""{"type":"PONG"}"""))
        assertNull(
            ChannelPointsPubSubParser.parseRedemptionMessage(
                """{"type":"custom-reward-updated","data":{}}""",
            ),
        )
    }
}
