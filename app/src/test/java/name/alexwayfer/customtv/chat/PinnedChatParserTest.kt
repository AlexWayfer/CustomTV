package name.alexwayfer.customtv.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PinnedChatParserTest {
    /** A PubSub pin of CoolUser's message with an emote, by ModUser. */
    private val pinMessage = """
        {"type":"pin-message","data":{"id":"pin-1","ends_at":"2026-09-18T21:00:00.000Z",
        "pinned_by":{"login":"moduser","display_name":"ModUser"},
        "message":{"id":"msg-1","sent_at":"2026-09-18T20:58:00.000Z",
        "sender":{"id":"4242","login":"cooluser","display_name":"CoolUser","chat_color":"#FF4500"},
        "content":{"text":"Hello Kappa","fragments":[{"text":"Hello "},
        {"text":"Kappa","emoticon":{"emoticonID":"25"}}]}}}}
    """.trimIndent()

    /** GQL's pin of the streamer's message, by a moderator, with both their badges. */
    private val gqlPinnedBody = """
        {"data":{"channel":{"pinnedChatMessages":{"edges":[{"node":{"id":"pin-gql","endsAt":null,
        "pinnedBy":{"login":"moduser","displayName":"ModUser","displayBadges":[{"setID":"moderator","version":"1"}]},
        "pinnedMessage":{"id":"msg-gql","sentAt":"2026-09-18T20:00:00.000Z",
        "sender":{"id":"99","login":"streamer","displayName":"Streamer","chatColor":"#1E90FF",
        "displayBadges":[{"setID":"broadcaster","version":"1"}]},
        "content":{"text":"Be right back","fragments":[{"text":"Be right back"}]}}}}]}}}}
    """.trimIndent()

    @Test
    fun parsePinMessageWithEmoteFragment() {
        val frame = JSONObject()
            .put("type", "MESSAGE")
            .put("data", JSONObject().put("topic", "pinned-chat-updates-v1.117474239").put("message", pinMessage))
            .toString()
        val update = PinnedChatParser.parseFrame(frame) as PinnedChatUpdate.Set
        assertEquals("pin-1", update.pin.pinId)
        assertEquals("ModUser", update.pin.pinnedBy?.displayName)
        assertEquals("moduser", update.pin.pinnedBy?.login)
        assertEquals(Instant.parse("2026-09-18T21:00:00.000Z").toEpochMilli(), update.pin.endsAtMillis)
        val message = update.pin.message
        assertEquals("msg-1", message.id)
        assertEquals("CoolUser", message.displayName)
        assertEquals("cooluser", message.userLogin)
        assertEquals("4242", message.userId)
        assertEquals("Hello Kappa", message.rawText)
        assertEquals(2, message.parts.size)
        assertEquals(ChatPart.Text("Hello "), message.parts[0])
        val emote = message.parts[1] as ChatPart.Emote
        assertEquals("Kappa", emote.name)
        assertTrue(emote.url.contains("/25/"))
    }

    @Test
    fun pubSubSenderBadgesUseIdAsSet() {
        val inner = """
            {"type":"pin-message","data":{"id":"pin-2","message":{"id":"msg-2",
            "sender":{"id":"4242","display_name":"CoolUser",
            "badges":[{"id":"subscriber","version":"6"},{"id":"vip","version":"1"}]},
            "content":{"text":"hi"}}}}
        """.trimIndent()
        val update = PinnedChatParser.parseUpdate(inner) as PinnedChatUpdate.Set
        assertEquals(
            listOf(ChatBadge("subscriber", "6"), ChatBadge("vip", "1")),
            update.pin.message.badges,
        )
    }

    @Test
    fun parseUnpinMessage() {
        val inner = """{"type":"unpin-message","data":{"id":"pin-1"}}"""
        val update = PinnedChatParser.parseUpdate(inner) as PinnedChatUpdate.Clear
        assertEquals("pin-1", update.pinId)
    }

    @Test
    fun parseUpdateMessageDuration() {
        val inner = """{"type":"update-message","data":{"id":"pin-1","ends_at":"2026-09-18T21:10:00.000Z"}}"""
        val update = PinnedChatParser.parseUpdate(inner) as PinnedChatUpdate.Duration
        assertEquals("pin-1", update.pinId)
        assertEquals(Instant.parse("2026-09-18T21:10:00.000Z").toEpochMilli(), update.endsAtMillis)
    }

    @Test
    fun parseGqlPinnedMessage() {
        val pin = checkNotNull(PinnedChatParser.parseGqlBody(gqlPinnedBody))
        assertEquals("pin-gql", pin.pinId)
        assertEquals("ModUser", pin.pinnedBy?.displayName)
        assertEquals(listOf(ChatBadge("moderator", "1")), pin.pinnedBy?.badges)
        assertNull(pin.endsAtMillis)
        assertEquals("Streamer", pin.message.displayName)
        assertEquals("streamer", pin.message.userLogin)
        assertEquals("99", pin.message.userId)
        assertEquals("Be right back", pin.message.rawText)
        assertEquals(listOf(ChatBadge("broadcaster", "1")), pin.message.badges)
        assertEquals(ChatPart.Text("Be right back"), pin.message.parts.single())
    }

    @Test
    fun ignoreChannelPointsFrame() {
        val frame = JSONObject()
            .put("type", "MESSAGE")
            .put(
                "data",
                JSONObject()
                    .put("topic", "community-points-channel-v1.1")
                    .put("message", """{"type":"reward-redeemed","data":{}}"""),
            )
            .toString()
        assertNull(PinnedChatParser.parseFrame(frame))
    }

    @Test
    fun pinRoleBadgePrefersBroadcaster() {
        val owner = PinnedBy(
            login = "streamer",
            displayName = "Streamer",
            badges = listOf(
                ChatBadge("subscriber", "12"),
                ChatBadge("broadcaster", "1"),
                ChatBadge("moderator", "1"),
            ),
        )
        assertEquals(ChatBadge("broadcaster", "1"), owner.pinRoleBadge("streamer"))
        val inferredOwner = PinnedBy(login = "streamer", displayName = "Streamer")
        assertEquals(ChatBadge("broadcaster", "1"), inferredOwner.pinRoleBadge("streamer"))
        val mod = PinnedBy(
            login = "moduser",
            displayName = "ModUser",
            badges = listOf(ChatBadge("subscriber", "3"), ChatBadge("moderator", "1")),
        )
        assertEquals(ChatBadge("moderator", "1"), mod.pinRoleBadge("streamer"))
        val inferredMod = PinnedBy(login = "moduser", displayName = "ModUser")
        assertEquals(ChatBadge("moderator", "1"), inferredMod.pinRoleBadge("streamer"))
    }
}
