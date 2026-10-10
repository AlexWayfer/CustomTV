package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatSendTest {
    @Test
    fun aBlankMessageIsNotSent() {
        assertNull(chatMessageToSend("  "))
        assertNull(chatMessageToSend(""))
    }

    @Test
    fun surroundingSpacesAreRemovedBeforeSending() {
        assertEquals("hello", chatMessageToSend("  hello  "))
    }

    @Test
    fun aMessageLongerThan500CharactersIsCutTo500() {
        val sent = chatMessageToSend("a".repeat(CHAT_MESSAGE_MAX_LENGTH + 20))
        assertEquals(CHAT_MESSAGE_MAX_LENGTH, sent?.length)
    }

    @Test
    fun aSentResponseCountsAsSent() {
        val body = """{"data":[{"message_id":"m1","is_sent":true,"drop_reason":null}]}"""
        assertEquals(ChatSendResult.Sent, parseChatSendResponse(200, body))
    }

    @Test
    fun aDroppedMessageKeepsTheDropCodeAndReason() {
        val body = """{"data":[{"message_id":"m1","is_sent":false,"drop_reason":{"code":"msg_rejected","message":"no"}}]}"""
        assertEquals(ChatSendResult.Dropped("msg_rejected", "no"), parseChatSendResponse(200, body))
    }

    @Test
    fun aRejectedRequestKeepsTheCodeAndTwitchText() {
        assertEquals(ChatSendResult.Rejected(401, "no"), parseChatSendResponse(401, """{"message":"no"}"""))
    }

    @Test
    fun aDroppedMessageShowsTwitchReason() {
        val reason = "This room is in followers-only mode. Follow to join the community!"
        assertEquals(ChatSendError.Twitch(reason), chatSendError(ChatSendResult.Dropped("followers_only", reason)))
        assertEquals(ChatSendError.Failed, chatSendError(ChatSendResult.Dropped("unknown", "")))
    }

    @Test
    fun documentedRefusalsGetTheAppsOwnWords() {
        assertEquals(ChatSendError.LogIn, chatSendError(ChatSendResult.Rejected(401, "The access token is not valid.")))
        assertEquals(ChatSendError.NotAllowed, chatSendError(ChatSendResult.Rejected(403, "not permitted")))
        assertEquals(ChatSendError.TooLong, chatSendError(ChatSendResult.Rejected(422, "too large")))
        assertEquals(ChatSendError.TooFast, chatSendError(ChatSendResult.Rejected(429, "")))
    }

    @Test
    fun anotherRefusalShowsTwitchTextOrTheGeneralError() {
        assertEquals(ChatSendError.Twitch("bad"), chatSendError(ChatSendResult.Rejected(400, "bad")))
        assertEquals(ChatSendError.Failed, chatSendError(ChatSendResult.Rejected(400, "")))
        assertEquals(ChatSendError.Failed, chatSendError(ChatSendResult.Unavailable))
        assertNull(chatSendError(ChatSendResult.Sent))
    }

    @Test
    fun aServerFailureStaysUnavailable() {
        assertEquals(ChatSendResult.Unavailable, parseChatSendResponse(503, ""))
        assertEquals(ChatSendResult.Unavailable, parseChatSendResponse(200, """{"data":[]}"""))
    }
}
