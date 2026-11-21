package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import name.alexwayfer.customtv.data.ChatSendResult
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchIrcSenderTest {
    private val opened = mutableListOf<FakeIrcSocket>()
    private val client = WebSocket.Factory { request, listener ->
        FakeIrcSocket(request, listener).also(opened::add)
    }
    private val sender = TwitchIrcSender("target", "viewer", client, nowMillis = { 1_000L })

    @Test
    fun aReplyWaitsForRegistrationAndItsOwnChannelsAcknowledgment() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "Hi", "reply-id") }
        val socket = opened.single()
        socket.open()
        assertEquals(
            listOf("CAP REQ :twitch.tv/tags twitch.tv/commands", "PASS oauth:test-token", "NICK viewer"),
            socket.sent,
        )

        socket.receive(":tmi.twitch.tv 001 viewer :Welcome")
        assertEquals("@reply-parent-msg-id=reply-id PRIVMSG #target :Hi", socket.sent.last())
        socket.receive("@id=other :tmi.twitch.tv USERSTATE #elsewhere")
        assertFalse(result.isCompleted)

        socket.receive("@id=sent :tmi.twitch.tv USERSTATE #target")
        assertEquals(ChatSendResult.Sent, result.await())
    }

    @Test
    fun aTimeoutNoticeProvidesTheRemainingTimeWithoutAClearChatEvent() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "Hi", null) }
        val socket = opened.single()
        socket.register()
        val refusal = "You are banned from talking in target for 37 more seconds."

        socket.receive("@msg-id=msg_timedout :tmi.twitch.tv NOTICE #target :$refusal")

        assertEquals(ChatSendResult.Dropped("user_timed_out", refusal, timeoutEndsAtMillis = 38_000), result.await())
    }

    @Test
    fun aTimeoutWithOneSecondLeftAndARefusalWithoutANumberAreBothRecognized() {
        val sample = "@msg-id=msg_timedout :tmi.twitch.tv NOTICE #target :"
        val withSeconds = requireNotNull(IrcLineParser.parse(sample + "You are timed out for 1 more second."))
        val withoutSeconds = requireNotNull(IrcLineParser.parse(sample + "You are timed out."))

        assertEquals(
            ChatSendResult.Dropped("user_timed_out", "You are timed out for 1 more second.", 2_000),
            ircChatSendResult(withSeconds, 1_000),
        )
        assertEquals(
            ChatSendResult.Dropped("user_timed_out", "You are timed out."),
            ircChatSendResult(withoutSeconds, 1_000),
        )
    }

    @Test
    fun retryUsesTheSameAuthenticatedSocketAfterAnAnsweredSend() = runTest {
        val first = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "First", null) }
        val socket = opened.single()
        socket.register()
        socket.receive("@id=one :tmi.twitch.tv USERSTATE #target")
        assertEquals(ChatSendResult.Sent, first.await())

        val second = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "Second", null) }
        assertEquals(1, opened.size)
        assertEquals("PRIVMSG #target :Second", socket.sent.last())
        socket.receive("@id=two :tmi.twitch.tv USERSTATE #target")
        assertEquals(ChatSendResult.Sent, second.await())
    }

    @Test
    fun cancellingAnUnansweredSendDiscardsItsSocketAndDoesNotReplayIt() = runTest {
        val first = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "First", null) }
        val old = opened.single()
        old.register()
        first.cancelAndJoin()
        assertTrue(old.cancelled)

        val second = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "Second", null) }
        val current = opened.last()
        current.register()
        old.receive("@msg-id=msg_timedout :tmi.twitch.tv NOTICE #target :You are timed out for 20 more seconds.")
        assertFalse(second.isCompleted)
        assertEquals(listOf("PRIVMSG #target :Second"), current.sent.filter { it.startsWith("PRIVMSG") })
        current.receive(":tmi.twitch.tv USERSTATE #target")
        assertEquals(ChatSendResult.Sent, second.await())
    }

    @Test
    fun aRejectedIrcLoginRequestsSignInAndSendsNoMessage() = runTest {
        val result = async(start = CoroutineStart.UNDISPATCHED) { sender.send("test-token", "Hi", null) }
        val socket = opened.single()
        socket.open()
        socket.receive(":tmi.twitch.tv NOTICE * :Login authentication failed")

        assertEquals(ChatSendResult.Rejected(401, ""), result.await())
        assertTrue(socket.cancelled)
        assertFalse(socket.sent.any { it.startsWith("PRIVMSG") })
    }

    @Test
    fun aLineBreakInTheDraftCannotBecomeASecondIrcCommand() = runTest {
        assertEquals(ChatSendResult.Unavailable, sender.send("test-token", "First line\r\nSecond line", null))
        assertTrue(opened.isEmpty())
    }
}
