package name.alexwayfer.customtv.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelShoutoutTest {
    @Test
    fun createGivesTheShoutoutWithASizedAvatar() {
        val shoutout = requireNotNull(parseShoutoutPubSubMessage(createMessage(), nowMillis = 1_000L))

        assertEquals("shoutout-1", shoutout.id)
        assertEquals("100", shoutout.channelId)
        assertEquals("200", shoutout.targetId)
        assertEquals("target", shoutout.targetLogin)
        assertEquals("Target", shoutout.targetDisplayName)
        assertEquals("https://example.com/avatar-150x150.png", shoutout.targetAvatarUrl)
        assertEquals("helper", shoutout.sourceLogin)
        assertEquals(1_000L, shoutout.receivedAtMillis)
    }

    @Test
    fun aBlankDisplayNameFallsBackToTheLogin() {
        val shoutout = parseShoutoutPubSubMessage(createMessage(displayName = ""), nowMillis = 0L)

        assertEquals("target", shoutout?.targetDisplayName)
    }

    @Test
    fun anEmptyAvatarAndSourceLeaveThemOut() {
        val shoutout = parseShoutoutPubSubMessage(createMessage(avatar = "", sourceLogin = ""), nowMillis = 0L)

        assertNull(shoutout?.targetAvatarUrl)
        assertNull(shoutout?.sourceLogin)
    }

    @Test
    fun aShoutoutWithoutItsTargetOrIdIsDropped() {
        assertNull(parseShoutoutPubSubMessage(createMessage(targetLogin = ""), nowMillis = 0L))
        assertNull(parseShoutoutPubSubMessage(createMessage(targetId = ""), nowMillis = 0L))
        assertNull(parseShoutoutPubSubMessage(createMessage(shoutoutId = ""), nowMillis = 0L))
    }

    @Test
    fun anotherTypeOrTextIsNotAShoutout() {
        assertNull(parseShoutoutPubSubMessage(createMessage(type = "delete"), nowMillis = 0L))
        assertNull(parseShoutoutPubSubMessage("not json", nowMillis = 0L))
    }

    @Test
    fun aFrameOfTheShoutoutTopicCarriesItsMessage() {
        val frame = pubSubFrame("shoutout.100", createMessage())

        assertEquals("shoutout-1", parseShoutoutPubSubFrame(frame, nowMillis = 0L)?.id)
        assertNull(parseShoutoutPubSubFrame(pubSubFrame("raid.100", createMessage()), nowMillis = 0L))
    }

    @Test
    fun onlyTheOpenChannelsShoutoutIsKept() {
        val shoutout = parseShoutoutPubSubMessage(createMessage(), nowMillis = 0L)

        assertEquals(shoutout, shoutoutForChannel(shoutout, channelId = "100"))
        assertNull(shoutoutForChannel(shoutout, channelId = "300"))
        assertNull(shoutoutForChannel(shoutout, channelId = null))
    }

    @Test
    fun thePlaqueLastsFortyFiveSecondsFromArrival() {
        val shoutout = parseShoutoutPubSubMessage(createMessage(), nowMillis = 1_000L)

        assertEquals(shoutout, visibleShoutout(shoutout, hiddenId = null, nowMillis = 45_999L))
        assertNull(visibleShoutout(shoutout, hiddenId = null, nowMillis = 46_000L))
        assertEquals(45_000L, shoutoutPlaqueRemainingMillis(requireNotNull(shoutout), nowMillis = 0L))
        assertEquals(0L, shoutoutPlaqueRemainingMillis(shoutout, nowMillis = 99_000L))
    }

    @Test
    fun aHiddenShoutoutStaysHidden() {
        val shoutout = parseShoutoutPubSubMessage(createMessage(), nowMillis = 0L)

        assertNull(visibleShoutout(shoutout, hiddenId = "shoutout-1", nowMillis = 0L))
        assertNull(visibleShoutout(null, hiddenId = null, nowMillis = 0L))
    }

    @Test
    fun theSourceShowsTheDisplayNameTheChatKnows() {
        assertEquals("Helper", shoutoutSourceName("helper", mapOf("helper" to "Helper")))
        assertEquals("helper", shoutoutSourceName("helper", emptyMap()))
        assertNull(shoutoutSourceName(null, mapOf("helper" to "Helper")))
    }

    @Test
    fun theChatLineNamesTheChannelAndWhoGaveTheShoutout() {
        val shoutout = requireNotNull(parseShoutoutPubSubMessage(createMessage(), nowMillis = 5_000L))

        val row = shoutoutChatRow(shoutout, sourceName = "Helper")

        assertEquals("shoutout-shoutout-1", row.id)
        assertEquals(ChatEventKind.System, row.eventKind)
        assertEquals(5_000L, row.timestampMillis)
        assertEquals("", row.userLogin)
        assertEquals(
            ChatShoutoutNotice(targetLogin = "target", targetId = "200", targetName = "Target", sourceName = "Helper"),
            row.shoutoutNotice,
        )
    }

    private fun createMessage(
        type: String = "create",
        shoutoutId: String = "shoutout-1",
        targetId: String = "200",
        targetLogin: String = "Target",
        displayName: String = "Target",
        avatar: String = "https://example.com/avatar-%s.png",
        sourceLogin: String = "helper",
    ): String = """
        {"type":"$type","data":{
          "broadcasterUserID":"100",
          "targetUserID":"$targetId",
          "targetLogin":"$targetLogin",
          "targetUserProfileImageURL":"$avatar",
          "sourceUserID":"400",
          "sourceLogin":"$sourceLogin",
          "shoutoutID":"$shoutoutId",
          "targetUserDisplayName":"$displayName",
          "targetUserCTAInfo":"",
          "targetUserPrimaryColorHex":""
        }}
    """.trimIndent()

    private fun pubSubFrame(topic: String, message: String): String =
        JSONObject()
            .put("type", "MESSAGE")
            .put("data", JSONObject().put("topic", topic).put("message", message))
            .toString()
}
