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

class BttvEventClient(
    client: OkHttpClient = pushSocketHttpClient(),
) {
    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<ChatMessage> = _messages

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "bttv-events").apply { isDaemon = true }
    }

    private val socket = EmoteEventSocket(
        open = { listener -> client.newWebSocket(Request.Builder().url(EVENTS_URL).build(), listener) },
        schedule = eventSubSchedule(reconnectExecutor),
        onOpened = ::sendJoin,
        onFrame = { _, _, text -> BttvEventParser.parseFrame(text)?.let { _messages.tryEmit(it) } },
    )

    fun listen(channelId: String) = socket.listen(channelId)

    fun disconnect() = socket.disconnect()

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun sendJoin(webSocket: WebSocket, channelId: String) {
        val payload = JSONObject()
            .put("name", "join_channel")
            .put("data", JSONObject().put("name", "twitch:$channelId"))
        webSocket.send(payload.toString())
    }

    private companion object {
        const val EVENTS_URL = "wss://sockets.betterttv.net/ws"
    }
}
