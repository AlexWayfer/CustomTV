package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import name.alexwayfer.customtv.data.eventSubSchedule
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import java.util.concurrent.Executors

class SevenTvEventClient(
    client: OkHttpClient = pushSocketHttpClient(),
) {
    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<ChatMessage> = _messages

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "7tv-events").apply { isDaemon = true }
    }

    private val socket = EmoteEventSocket(
        open = { listener -> client.newWebSocket(Request.Builder().url(EVENTS_URL).build(), listener) },
        schedule = eventSubSchedule(reconnectExecutor),
        onFrame = ::handleFrame,
    )

    fun listen(setId: String) = socket.listen(setId)

    fun disconnect() = socket.disconnect()

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun handleFrame(webSocket: WebSocket, setId: String, text: String) {
        when (val frame = SevenTvEventParser.parseFrame(text)) {
            is SevenTvFrame.Hello -> sendSubscribe(webSocket, setId)
            is SevenTvFrame.Dispatch -> frame.messages.forEach { _messages.tryEmit(it) }
            SevenTvFrame.Reconnect -> socket.reconnect()
            SevenTvFrame.Ignore -> Unit
        }
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

    private companion object {
        const val EVENTS_URL = "wss://events.7tv.io/v3"
        const val OP_SUBSCRIBE = 35
    }
}
