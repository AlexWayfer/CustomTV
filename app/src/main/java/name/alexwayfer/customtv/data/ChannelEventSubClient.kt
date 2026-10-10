package name.alexwayfer.customtv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.chat.socketTaskAllowed
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class ChannelEventSubClient(
    private val clientId: String,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
) {
    private val _events = MutableSharedFlow<StreamStatusEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<StreamStatusEvent> = _events
    private val _userNotifications = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Raw `notification` messages of the subscriptions that name the signed-in user: moderator and own message ones. */
    internal val userNotifications: SharedFlow<String> = _userNotifications
    private val _moderating = MutableStateFlow(false)

    /** True while at least one moderator subscription on the current socket is active. */
    internal val moderating: StateFlow<Boolean> = _moderating
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "channel-eventsub").apply { isDaemon = true }
    }
    private val json = "application/json; charset=utf-8".toMediaType()

    private var webSocket: WebSocket? = null
    @Volatile
    private var sessionId: String? = null
    @Volatile
    private var keepaliveSeconds: Long? = null
    @Volatile
    private var silenceCheck: ScheduledFuture<*>? = null
    private var broadcasterId: String? = null
    private var accessToken: String? = null
    private var userId: String? = null
    @Volatile
    private var closedByUser = false

    /** [userId] is the signed-in user: their own held messages, and what moderators see when they moderate the channel. */
    fun listen(broadcasterId: String, accessToken: String, userId: String?) {
        val id = broadcasterId.trim()
        val token = accessToken.trim()
        val user = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (id.isEmpty() || token.isEmpty()) return
        if (this.broadcasterId == id && this.accessToken == token && this.userId == user &&
            webSocket != null && !closedByUser
        ) {
            return
        }
        closedByUser = false
        this.broadcasterId = id
        this.accessToken = token
        this.userId = user
        disconnectSocket()
        AppLog.i(TAG, "connect")
        openSocket()
    }

    fun disconnect() {
        closedByUser = true
        broadcasterId = null
        accessToken = null
        userId = null
        disconnectSocket()
    }

    /** Asks for the moderator subscriptions again on the open socket, such as after the user became a moderator. */
    internal fun retryModeratorSubscriptions() {
        if (_moderating.value) return
        val session = sessionId ?: return
        AppLog.i(TAG, "retry moderator subscriptions")
        subscribe(session, moderatorEventSubTypes())
    }

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
    }

    private fun openSocket() {
        val request = Request.Builder().url(SOCKET_URL).build()
        webSocket = http.newWebSocket(request, SocketListener())
    }

    private fun disconnectSocket() {
        val current = webSocket
        webSocket = null
        sessionId = null
        _moderating.value = false
        silenceCheck?.cancel(false)
        silenceCheck = null
        current?.cancel()
    }

    /** Restarts the wait for the next message on [socket]: a socket silent past its keepalive timeout is dead. */
    private fun watchSilence(socket: WebSocket) {
        silenceCheck?.cancel(false)
        if (!socketTaskAllowed(closedByUser, reconnectExecutor.isShutdown)) return
        val timeoutSeconds = eventSubSilenceTimeoutSeconds(keepaliveSeconds)
        silenceCheck = try {
            reconnectExecutor.schedule(
                {
                    if (!closedByUser && webSocket == socket) {
                        AppLog.w(TAG, "silent for ${timeoutSeconds}s, reconnect")
                        disconnectSocket()
                        scheduleReconnect()
                    }
                },
                timeoutSeconds,
                TimeUnit.SECONDS,
            )
        } catch (_: RejectedExecutionException) {
            null
        }
    }

    private fun subscribe(sessionId: String, types: List<EventSubType>) {
        val id = broadcasterId ?: return
        val token = accessToken ?: return
        val user = userId
        types.forEach { type ->
            if (type.condition != EventSubCondition.Broadcaster && user == null) return@forEach
            scope.launch {
                subscribeOnce(token, id, user, sessionId, type, attempt = 0)
            }
        }
    }

    private fun subscribeOnce(
        token: String,
        broadcasterId: String,
        userId: String?,
        sessionId: String,
        type: EventSubType,
        attempt: Int,
    ) {
        val condition = JSONObject().put("broadcaster_user_id", broadcasterId)
        type.condition.userField?.let { field -> condition.put(field, userId) }
        val payload = JSONObject()
            .put("type", type.name)
            .put("version", type.version)
            .put("condition", condition)
            .put(
                "transport",
                JSONObject().put("method", "websocket").put("session_id", sessionId),
            )
        val request = helixRequest(SUBSCRIBE_URL, token, clientId)
            .post(payload.toString().toRequestBody(json))
            .build()
        val name = type.name
        try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (eventSubSubscribeOutcome(response.code, type.condition, sessionId, this.sessionId, twitchAuthMessage(body))) {
                    EventSubSubscribeOutcome.Subscribed -> {
                        AppLog.i(TAG, "subscribed $name")
                        if (type.condition == EventSubCondition.Moderator && sessionId == this.sessionId) _moderating.value = true
                    }
                    EventSubSubscribeOutcome.NotModerator -> AppLog.i(TAG, "not a moderator HTTP 403 $name")
                    EventSubSubscribeOutcome.LoginRejected -> {
                        AppLog.w(TAG, "subscribe rejected HTTP ${response.code} $name")
                        // 401 is an expired login; stream status needs no permission, so a 403 is unexpected.
                        if (response.code == 403) reportSubscribeRejected(response.code, name, body)
                        disconnect()
                    }
                    EventSubSubscribeOutcome.Outdated ->
                        AppLog.i(TAG, "subscribe outlived its session HTTP ${response.code} $name")
                    EventSubSubscribeOutcome.TransportLimit -> {
                        val delaySeconds = eventSubTransportRetrySeconds(keepaliveSeconds, attempt)
                        AppLog.w(TAG, "subscribe hit the socket limit HTTP ${response.code} $name, retry in ${delaySeconds}s")
                        // The first refusal right after a reconnect is expected; a refused retry means live extra sockets.
                        if (attempt > 0) reportSubscribeRejected(response.code, name, body)
                        scope.launch {
                            delay(delaySeconds.seconds)
                            if (sessionId == this@ChannelEventSubClient.sessionId) {
                                subscribeOnce(token, broadcasterId, userId, sessionId, type, attempt + 1)
                            }
                        }
                    }
                    EventSubSubscribeOutcome.Rejected -> {
                        AppLog.w(TAG, "subscribe rejected HTTP ${response.code} $name")
                        reportSubscribeRejected(response.code, name, body)
                    }
                    EventSubSubscribeOutcome.Failed -> AppLog.w(TAG, "subscribe failed HTTP ${response.code} $name")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(TAG, "subscribe failed $name: ${error.javaClass.simpleName}")
        }
    }

    /** EventSub answers 429 when the subscriptions cost too much or the login has too many sockets: a refusal, not a request rate. */
    private fun reportSubscribeRejected(httpCode: Int, type: String, body: String) {
        Diagnostics.reportRejected(TAG, "helix/eventsub/subscriptions $type", httpCode, twitchAuthMessage(body))
    }

    private fun scheduleReconnect() {
        if (closedByUser) return
        val id = broadcasterId ?: return
        if (!socketTaskAllowed(closedByUser, reconnectExecutor.isShutdown)) return
        try {
            reconnectExecutor.schedule(
                {
                    if (!closedByUser && broadcasterId == id && webSocket == null) {
                        AppLog.i(TAG, "reconnect")
                        openSocket()
                    }
                },
                2,
                TimeUnit.SECONDS,
            )
        } catch (_: RejectedExecutionException) {
        }
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket != this@ChannelEventSubClient.webSocket) return
            watchSilence(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@ChannelEventSubClient.webSocket) return
            eventSubSessionId(text)?.let { sessionId ->
                AppLog.i(TAG, "welcome")
                this@ChannelEventSubClient.sessionId = sessionId
                keepaliveSeconds = eventSubKeepaliveSeconds(text)
                watchSilence(webSocket)
                subscribe(sessionId, STREAM_STATUS_EVENTSUB_TYPES + OWN_MESSAGE_HOLD_EVENTSUB_TYPES + moderatorEventSubTypes())
                return
            }
            watchSilence(webSocket)
            val metadataType = runCatching {
                JSONObject(text).optJSONObject("metadata")?.optString("message_type")
            }.getOrNull()
            when (metadataType) {
                "session_reconnect" -> {
                    AppLog.i(TAG, "session ended")
                    disconnectSocket()
                    scheduleReconnect()
                }
                "revocation" -> {
                    val type = eventSubMessageSubscriptionType(text, "revocation")
                    AppLog.w(TAG, "subscription revoked $type")
                    // Twitch revokes the moderator subscriptions when the user loses moderator status.
                    if (moderatorEventSubTypes().any { it.name == type }) _moderating.value = false
                }
                "notification" -> {
                    val type = eventSubMessageSubscriptionType(text, "notification")
                    if (STREAM_STATUS_EVENTSUB_TYPES.none { it.name == type }) {
                        // Suspicious chatter messages come with every message they send; the rest are rare.
                        if (type != "channel.suspicious_user.message") AppLog.i(TAG, "notification $type")
                        _userNotifications.tryEmit(text)
                    } else {
                        parseStreamStatusNotification(text)?.let { _events.tryEmit(it) }
                    }
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@ChannelEventSubClient.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@ChannelEventSubClient.webSocket) return
            this@ChannelEventSubClient.webSocket = null
            this@ChannelEventSubClient.sessionId = null
            AppLog.i(TAG, "closed $code")
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@ChannelEventSubClient.webSocket) return
            this@ChannelEventSubClient.webSocket = null
            this@ChannelEventSubClient.sessionId = null
            AppLog.w(TAG, "failed: ${t.javaClass.simpleName}")
            scheduleReconnect()
        }
    }

    private companion object {
        const val TAG = "ChannelEventSub"
        const val SOCKET_URL = "wss://eventsub.wss.twitch.tv/ws"
        const val SUBSCRIBE_URL = "https://api.twitch.tv/helix/eventsub/subscriptions"
    }
}
