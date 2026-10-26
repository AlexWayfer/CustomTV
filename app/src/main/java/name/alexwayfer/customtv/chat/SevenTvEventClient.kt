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

class SevenTvEventClient(
    client: OkHttpClient = pushSocketHttpClient(),
) {
    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<ChatMessage> = _messages

    // Kept through a reconnect of the same channel: 7TV sends a set to a socket once, when its chatter is first seen.
    private val _personal = MutableStateFlow(PersonalEmotes())
    internal val personal: StateFlow<PersonalEmotes> = _personal

    // Kept through a reconnect the same way; the paints and badges themselves are kept for the next channel too.
    private val _cosmetics = MutableStateFlow(SevenTvCosmetics())
    internal val cosmetics: StateFlow<SevenTvCosmetics> = _cosmetics

    private val _subscribed = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** The channel ID each new socket follows once it has subscribed, so a presence sent then reaches it too. */
    internal val subscribed: SharedFlow<String> = _subscribed

    @Volatile
    private var channelId: String? = null

    @Volatile
    private var channelSetId: String? = null

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "7tv-events").apply { isDaemon = true }
    }

    private val socket = EmoteEventSocket(
        open = { listener -> client.newWebSocket(Request.Builder().url(EVENTS_URL).build(), listener) },
        schedule = eventSubSchedule(reconnectExecutor),
        onFrame = ::handleFrame,
    )

    /**
     * Follows the personal emotes of the chatters of [channelId], and the channel's own emote set [setId] once it is
     * known; a channel without a 7TV set has chatters with personal emotes too.
     */
    fun listen(channelId: String, setId: String?) {
        val sameChannel = this.channelId == channelId
        if (!sameChannel) {
            _personal.value = PersonalEmotes()
            _cosmetics.update { it.withoutChatters() }
        }
        val newSet = setId != channelSetId
        this.channelId = channelId
        channelSetId = setId
        socket.listen(channelId)
        // A new socket subscribes as it opens; the one already open for this channel hears of the set now.
        if (sameChannel && newSet && setId != null) socket.send(setSubscription(setId))
    }

    fun disconnect() = socket.disconnect()

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun handleFrame(webSocket: WebSocket, channelId: String, text: String) {
        when (val frame = SevenTvEventParser.parseFrame(text, channelSetId)) {
            is SevenTvFrame.Hello -> sendSubscribe(webSocket, channelId)
            is SevenTvFrame.Dispatch -> frame.messages.forEach { _messages.tryEmit(it) }
            is SevenTvFrame.Personal -> _personal.update { it.apply(frame.event) }
            is SevenTvFrame.Cosmetic -> _cosmetics.update { it.apply(frame.event) }
            SevenTvFrame.Reconnect -> socket.reconnect()
            SevenTvFrame.Ignore -> Unit
        }
    }

    private fun sendSubscribe(webSocket: WebSocket, channelId: String) {
        channelSetId?.let { webSocket.send(setSubscription(it)) }
        // Any subscription to the channel brings the sets, the paints, the badges, and the entitlements of the chatters
        // 7TV sees there; the second one asks for the entitlements taken back too.
        val inChannel = JSONObject().put("ctx", "channel").put("platform", "TWITCH").put("id", channelId)
        webSocket.send(subscribe("entitlement.create", inChannel))
        webSocket.send(subscribe("entitlement.delete", inChannel))
        _subscribed.tryEmit(channelId)
    }

    private fun setSubscription(setId: String): String =
        subscribe("emote_set.update", JSONObject().put("object_id", setId))

    private fun subscribe(type: String, condition: JSONObject): String = JSONObject()
        .put("op", OP_SUBSCRIBE)
        .put("d", JSONObject().put("type", type).put("condition", condition))
        .toString()

    private companion object {
        const val EVENTS_URL = "wss://events.7tv.io/v3"
        const val OP_SUBSCRIBE = 35
    }
}
