package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TwitchPubSubClient(
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

    private val _pins = MutableSharedFlow<PinnedChatUpdate>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val pins: SharedFlow<PinnedChatUpdate> = _pins

    private val _raids = MutableSharedFlow<RaidPubSubEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val raids: SharedFlow<RaidPubSubEvent> = _raids

    /** Channel IDs whose socket opened; frames sent while it was down are lost. */
    private val _opened = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val opened: SharedFlow<String> = _opened

    private val _polls = MutableSharedFlow<ChannelPoll>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val polls: SharedFlow<ChannelPoll> = _polls

    private val _predictions = MutableSharedFlow<ChannelPrediction>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val predictions: SharedFlow<ChannelPrediction> = _predictions

    private val _hypeTrains = MutableSharedFlow<HypeTrainEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val hypeTrains: SharedFlow<HypeTrainEvent> = _hypeTrains

    private val pingExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "pubsub-ping").apply { isDaemon = true }
    }

    private var webSocket: WebSocket? = null
    private var channelId: String? = null
    private var authToken: String? = null
    private var pingFuture: ScheduledFuture<*>? = null
    @Volatile
    private var closedByUser = false
    private var lastLoggedPollUpdateId: String? = null
    private var lastLoggedPollUpdateAtMillis = 0L

    fun listen(channelId: String, authToken: String? = null) {
        val normalized = channelId.trim()
        if (normalized.isEmpty()) return
        val token = authToken?.takeIf { it.isNotBlank() }
        if (this.channelId == normalized && this.authToken == token && webSocket != null && !closedByUser) return
        closedByUser = false
        this.channelId = normalized
        this.authToken = token
        disconnectSocket()
        openSocket()
    }

    fun disconnect() {
        closedByUser = true
        channelId = null
        authToken = null
        disconnectSocket()
    }

    fun close() {
        disconnect()
        pingExecutor.shutdownNow()
    }

    private fun openSocket() {
        val request = Request.Builder()
            .url(PUBSUB_URL)
            .build()
        webSocket = client.newWebSocket(request, SocketListener())
    }

    private fun disconnectSocket() {
        pingFuture?.cancel(false)
        pingFuture = null
        val current = webSocket
        webSocket = null
        current?.cancel()
    }

    /** Logs each kind of poll frame Twitch sends; updates, which come with every vote, only now and then. */
    private fun logPollFrame(text: String, poll: ChannelPoll?) {
        val type = pubSubMessage(text)?.optString("type").orEmpty()
        val now = System.currentTimeMillis()
        if (!pollFrameLogged(type, poll?.id, now, lastLoggedPollUpdateId, lastLoggedPollUpdateAtMillis)) return
        if (type == POLL_UPDATE) {
            lastLoggedPollUpdateId = poll?.id
            lastLoggedPollUpdateAtMillis = now
        }
        AppLog.i(TAG, if (poll == null) "poll frame $type unreadable" else "poll frame $type votes=${poll.totalVotes}")
    }

    private fun sendListen(webSocket: WebSocket, channelId: String, authToken: String?) {
        val topics = JSONArray()
            .put("community-points-channel-v1.$channelId")
            .put("pinned-chat-updates-v1.$channelId")
            .put("polls.$channelId")
            .put("predictions-channel-v1.$channelId")
            .put("hype-train-events-v2.$channelId")
        val data = JSONObject().put("topics", topics)
        if (!authToken.isNullOrBlank()) {
            topics.put("raid.$channelId")
            data.put("auth_token", authToken)
        }
        val payload = JSONObject()
            .put("type", "LISTEN")
            .put("nonce", UUID.randomUUID().toString())
            .put("data", data)
        webSocket.send(payload.toString())
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val id = channelId ?: return
            sendListen(webSocket, id, authToken)
            AppLog.i(TAG, "pubsub connected")
            _opened.tryEmit(id)
            pingFuture?.cancel(false)
            if (!socketTaskAllowed(closedByUser, pingExecutor.isShutdown)) return
            pingFuture = try {
                pingExecutor.scheduleWithFixedDelay(
                    {
                        if (webSocket === this@TwitchPubSubClient.webSocket) {
                            webSocket.send("""{"type":"PING"}""")
                        }
                    },
                    60,
                    60,
                    TimeUnit.SECONDS,
                )
            } catch (_: RejectedExecutionException) {
                null
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@TwitchPubSubClient.webSocket) return
            val type = runCatching { JSONObject(text).optString("type") }.getOrNull()
            when (type) {
                "PING" -> webSocket.send("""{"type":"PONG"}""")
                "RECONNECT" -> {
                    AppLog.i(TAG, "pubsub asked to reconnect")
                    reconnect()
                }
                "RESPONSE" -> {
                    val error = runCatching { JSONObject(text).optString("error") }.getOrNull().orEmpty()
                    if (error.isNotBlank()) AppLog.w(TAG, "pubsub listen rejected $error")
                }
                "MESSAGE" -> {
                    val topic = runCatching {
                        JSONObject(text).optJSONObject("data")?.optString("topic").orEmpty()
                    }.getOrDefault("")
                    when {
                        topic.startsWith("pinned-chat-updates-v1.") ->
                            PinnedChatParser.parseFrame(text)?.let { _pins.tryEmit(it) }
                        topic.startsWith("raid.") ->
                            parseRaidPubSubFrame(text, System.currentTimeMillis())?.let { _raids.tryEmit(it) }
                        topic.startsWith("polls.") -> {
                            val poll = ChannelPollParser.parseFrame(text, System.currentTimeMillis())
                            logPollFrame(text, poll)
                            poll?.let { _polls.tryEmit(it) }
                        }
                        topic.startsWith("predictions-channel-v1.") ->
                            ChannelPredictionParser.parseFrame(text, System.currentTimeMillis())
                                ?.let { _predictions.tryEmit(it) }
                        topic.startsWith("hype-train-events-v2.") ->
                            HypeTrainParser.parseFrame(text, System.currentTimeMillis())?.let { _hypeTrains.tryEmit(it) }
                        else ->
                            ChannelPointsPubSubParser.parseFrame(text)?.let { _messages.tryEmit(it) }
                    }
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@TwitchPubSubClient.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@TwitchPubSubClient.webSocket) return
            AppLog.w(TAG, "pubsub closed $code")
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@TwitchPubSubClient.webSocket) return
            AppLog.w(TAG, "pubsub failed ${t.javaClass.simpleName}")
            handleDisconnect()
        }
    }

    private fun reconnect() {
        if (closedByUser) return
        val id = channelId ?: return
        disconnectSocket()
        this.channelId = id
        openSocket()
    }

    private fun handleDisconnect() {
        if (closedByUser) return
        val id = channelId ?: return
        pingFuture?.cancel(false)
        pingFuture = null
        webSocket = null
        if (!socketTaskAllowed(closedByUser, pingExecutor.isShutdown)) return
        try {
            pingExecutor.schedule(
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
        const val TAG = "TwitchPubSub"
        const val PUBSUB_URL = "wss://pubsub-edge.twitch.tv/v1"
    }
}
