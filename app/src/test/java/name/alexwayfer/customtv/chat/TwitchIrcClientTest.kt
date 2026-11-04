package name.alexwayfer.customtv.chat

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class TwitchIrcClientTest {
    private class FakeSocket(private val request: Request) : WebSocket {
        var cancelled = false
        val sent = mutableListOf<String>()
        override fun request(): Request = request
        override fun queueSize(): Long = 0
        override fun send(text: String): Boolean {
            sent += text
            return true
        }
        override fun send(bytes: ByteString): Boolean = true
        override fun close(code: Int, reason: String?): Boolean = true
        override fun cancel() {
            cancelled = true
        }
    }

    private val opened = mutableListOf<Pair<FakeSocket, WebSocketListener>>()
    private var onOpening: (FakeSocket, WebSocketListener) -> Unit = { _, _ -> }
    private val client = TwitchIrcClient { request, listener ->
        FakeSocket(request).also {
            opened += it to listener
            onOpening(it, listener)
        }
    }

    @Test
    fun anImmediateFailureRequestsReconnectionAndDropsTheFailedSocket() {
        onOpening = { ws, listener -> listener.onFailure(ws, UnknownHostException(), null) }

        client.connect("target")

        assertEquals(ChatConnectionState.Reconnecting, client.state.value)
        assertTrue(opened.single().first.cancelled)
        client.connect("target")
        assertEquals(ChatConnectionState.Reconnecting, client.state.value)
        assertEquals(2, opened.size)
    }

    @Test
    fun anImmediateWelcomeJoinsTheRequestedChannel() {
        onOpening = { ws, listener ->
            listener.onOpen(ws, response(ws))
            listener.onMessage(ws, ":server 001 nick :Welcome\r\n")
        }

        client.connect("#Target")

        assertEquals(ChatConnectionState.Connected, client.state.value)
        assertEquals("JOIN #target", opened.single().first.sent.last())
    }

    @Test
    fun replacedSocketsCannotRegisterOrChangeConnectionState() {
        client.connect("first")
        val (old, listener) = opened.single()
        client.connect("second")

        listener.onOpen(old, response(old))
        listener.onMessage(old, ":server 001 nick :Welcome\r\n")
        listener.onFailure(old, UnknownHostException(), null)
        listener.onClosed(old, 1000, "")

        assertEquals(ChatConnectionState.Connecting, client.state.value)
        assertTrue(old.sent.isEmpty())
        assertTrue(old.cancelled)
    }

    @Test
    fun callbacksAfterPauseOrDisconnectCannotRestartTheChat() {
        client.connect("target")
        val (old, listener) = opened.single()
        client.pause()
        listener.onOpen(old, response(old))
        listener.onFailure(old, UnknownHostException(), null)
        assertEquals(ChatConnectionState.Disconnected, client.state.value)
        assertTrue(old.sent.isEmpty())

        client.connect("other")
        val (second, secondListener) = opened.last()
        client.disconnect()
        secondListener.onMessage(second, ":server 001 nick :Welcome\r\n")
        secondListener.onFailure(second, UnknownHostException(), null)
        assertEquals(ChatConnectionState.Disconnected, client.state.value)
        assertTrue(second.cancelled)
    }

    private fun response(socket: WebSocket): Response = Response.Builder()
        .request(socket.request())
        .protocol(Protocol.HTTP_1_1)
        .code(101)
        .message("")
        .build()
}
