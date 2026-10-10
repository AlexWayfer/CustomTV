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

class SevenTvEventClient(
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
        Thread(runnable, "7tv-events").apply { isDaemon = true }
    }

    private var webSocket: WebSocket? = null
    private var setId: String? = null
    @Volatile
    private var closedByUser = false

    fun listen(setId: String) {
        val normalized = setId.trim()
        if (normalized.isEmpty()) return
        if (this.setId == normalized && webSocket != null && !closedByUser) return
        closedByUser = false
        this.setId = normalized
        disconnectSocket()
        openSocket()
    }

    fun disconnect() {
        closedByUser = true
        setId = null
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

    private fun sendSubscribe(webSocket: WebSocket, setId: String) {
        val payload = JSONObject()
            .put("op", OP_SUBSCRIBE)
            .put(
                "d",
                JSONObject()
                    .put("type", "emote_set.update")
                    .put("condition", JSONObject().put("object_id", setId)),
            )
        webSocket.send(payload.toString())
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@SevenTvEventClient.webSocket) return
            when (val frame = SevenTvEventParser.parseFrame(text)) {
                is SevenTvFrame.Hello -> {
                    val id = setId ?: return
                    sendSubscribe(webSocket, id)
                }
                is SevenTvFrame.Dispatch -> {
                    frame.messages.forEach { _messages.tryEmit(it) }
                }
                SevenTvFrame.Reconnect -> reconnect()
                SevenTvFrame.Ignore -> Unit
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@SevenTvEventClient.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@SevenTvEventClient.webSocket) return
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@SevenTvEventClient.webSocket) return
            handleDisconnect()
        }
    }

    private fun reconnect() {
        if (closedByUser) return
        val id = setId ?: return
        disconnectSocket()
        this.setId = id
        openSocket()
    }

    private fun handleDisconnect() {
        if (closedByUser) return
        val id = setId ?: return
        webSocket = null
        if (!socketTaskAllowed(closedByUser, reconnectExecutor.isShutdown)) return
        try {
            reconnectExecutor.schedule(
                {
                    if (!closedByUser && setId == id && webSocket == null) {
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
        const val EVENTS_URL = "wss://events.7tv.io/v3"
        const val OP_SUBSCRIBE = 35
    }
}
