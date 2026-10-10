package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

class BttvEventClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
) {
    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<ChatMessage> = _messages

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "bttv-events").apply { isDaemon = true }
    }

    private var webSocket: WebSocket? = null
    private var channelId: String? = null
    @Volatile
    private var closedByUser = false

    fun listen(channelId: String) {
        val normalized = channelId.trim()
        if (normalized.isEmpty()) return
        if (this.channelId == normalized && webSocket != null && !closedByUser) return
        closedByUser = false
        this.channelId = normalized
        disconnectSocket()
        openSocket()
    }

    fun disconnect() {
        closedByUser = true
        channelId = null
        disconnectSocket()
    }

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun openSocket() {
        val request = Request.Builder()
            .url(EVENTS_URL)
            .build()
        webSocket = client.newWebSocket(request, SocketListener())
    }

    private fun disconnectSocket() {
        val current = webSocket
        webSocket = null
        current?.cancel()
    }

    private fun sendJoin(webSocket: WebSocket, channelId: String) {
        val payload = JSONObject()
            .put("name", "join_channel")
            .put("data", JSONObject().put("name", "twitch:$channelId"))
        webSocket.send(payload.toString())
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val id = channelId ?: return
            sendJoin(webSocket, id)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@BttvEventClient.webSocket) return
            BttvEventParser.parseFrame(text)?.let { _messages.tryEmit(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@BttvEventClient.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@BttvEventClient.webSocket) return
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@BttvEventClient.webSocket) return
            handleDisconnect()
        }
    }

    private fun handleDisconnect() {
        if (closedByUser) return
        val id = channelId ?: return
        webSocket = null
        if (!socketTaskAllowed(closedByUser, reconnectExecutor.isShutdown)) return
        try {
            reconnectExecutor.schedule(
                {
                    if (!closedByUser && channelId == id && webSocket == null) {
                        openSocket()
                    }
                },
                2,
                TimeUnit.SECONDS,
            )
        } catch (_: RejectedExecutionException) {
        }
    }

    private companion object {
        const val EVENTS_URL = "wss://sockets.betterttv.net/ws"
    }
}
