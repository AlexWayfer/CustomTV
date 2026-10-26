package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    // Kept through a reconnect of the same channel, as a chatter's emotes come only when their client joins.
    private val _personal = MutableStateFlow(PersonalEmotes())
    internal val personal: StateFlow<PersonalEmotes> = _personal

    @Volatile
    private var channelId: String? = null

    @Volatile
    private var selfUserId: String? = null

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "bttv-events").apply { isDaemon = true }
    }

    private val socket = EmoteEventSocket(
        open = { listener -> client.newWebSocket(Request.Builder().url(EVENTS_URL).build(), listener) },
        schedule = eventSubSchedule(reconnectExecutor),
        onOpened = ::sendJoin,
        onFrame = { _, _, text -> handleFrame(text) },
    )

    fun listen(channelId: String) {
        if (this.channelId != channelId) _personal.value = PersonalEmotes()
        this.channelId = channelId
        socket.listen(channelId)
    }

    /** The signed-in user, whom BTTV announces in the channel so their personal emotes show there; null when out. */
    fun setSelf(userId: String?) {
        val normalized = userId?.takeIf { it.isNotBlank() }
        if (selfUserId == normalized) return
        selfUserId = normalized
        val channel = channelId ?: return
        normalized?.let { socket.send(broadcastMe(channel, it)) }
    }

    fun disconnect() = socket.disconnect()

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun handleFrame(text: String) {
        when (val frame = BttvEventParser.parseFrame(text)) {
            is BttvFrame.EmoteChange -> _messages.tryEmit(frame.message)
            is BttvFrame.Personal -> _personal.update { it.apply(frame.event) }
            null -> Unit
        }
    }

    private fun sendJoin(webSocket: WebSocket, channelId: String) {
        val payload = JSONObject()
            .put("name", "join_channel")
            .put("data", JSONObject().put("name", "twitch:$channelId"))
        webSocket.send(payload.toString())
        selfUserId?.let { webSocket.send(broadcastMe(channelId, it)) }
    }

    /** What the BTTV extension sends as it joins a chat, so the others there get the user's personal emotes. */
    private fun broadcastMe(channelId: String, userId: String): String = JSONObject()
        .put("name", "broadcast_me")
        .put(
            "data",
            JSONObject()
                .put("provider", "twitch")
                .put("providerId", userId)
                .put("channel", "twitch:$channelId"),
        )
        .toString()

    private companion object {
        const val EVENTS_URL = "wss://sockets.betterttv.net/ws"
    }
}
