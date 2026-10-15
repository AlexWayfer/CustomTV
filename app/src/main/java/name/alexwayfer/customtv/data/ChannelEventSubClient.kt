package name.alexwayfer.customtv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

private const val TAG = "ChannelEventSub"

/**
 * EventSub answers 429 when the subscriptions cost too much or the login has too many sockets: a refusal, not a
 * request rate.
 */
private fun reportSubscribeRejected(httpCode: Int, type: String, body: String) {
    Diagnostics.reportRejected(TAG, "helix/eventsub/subscriptions $type", httpCode, twitchAuthMessage(body))
}

/**
 * [freshToken] reads the login's token again for each subscribing, refreshed when it is about to expire: the channel
 * may stay open for hours, longer than the token read when it opened.
 */
class ChannelEventSubClient(
    private val clientId: String,
    private val freshToken: suspend () -> String?,
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

    /**
     * Raw `notification` messages of the subscriptions that name the signed-in user: moderator and own message ones.
     */
    internal val userNotifications: SharedFlow<String> = _userNotifications
    private val _moderating = MutableStateFlow(false)

    /** True while at least one moderator subscription on the current socket is active. */
    internal val moderating: StateFlow<Boolean> = _moderating
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "channel-eventsub").apply { isDaemon = true }
    }
    private val json = "application/json; charset=utf-8".toMediaType()
    private val socket = EventSubSocket(
        TAG,
        open = { listener -> http.newWebSocket(Request.Builder().url(SOCKET_URL).build(), listener) },
        schedule = eventSubSchedule(reconnectExecutor),
        events = EventSubSocketEvents(
            onWelcome = { sessionId ->
                subscribe(
                    sessionId,
                    STREAM_STATUS_EVENTSUB_TYPES + OWN_MESSAGE_HOLD_EVENTSUB_TYPES + moderatorEventSubTypes(),
                )
            },
            onMessage = ::onMessage,
            onDisconnect = { _moderating.value = false },
        ),
    )

    private var broadcasterId: String? = null
    private var accessToken: String? = null
    private var userId: String? = null

    /**
     * The stream status events in the order they came: a new category is named in the app's language before it goes
     * on, and the events after it wait for it, so an offline never overtakes the title before it.
     */
    private val statusQueue = Channel<StreamStatusEvent>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (event in statusQueue) {
                val categoryId = (event as? StreamStatusEvent.Title)?.categoryId
                val names = if (categoryId == null) emptyMap() else CategoryNames.localized(listOf(categoryId))
                _events.emit(localizedStreamStatus(event, names))
            }
        }
    }

    /**
     * [userId] is the signed-in user: their own held messages, and what moderators see when they moderate the
     * channel.
     */
    fun listen(broadcasterId: String, accessToken: String, userId: String?) {
        val id = broadcasterId.trim()
        val token = accessToken.trim()
        val user = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (id.isEmpty() || token.isEmpty()) return
        val sameLogin = this.broadcasterId == id && this.accessToken == token && this.userId == user
        if (sameLogin && socket.connected) return
        this.broadcasterId = id
        this.accessToken = token
        this.userId = user
        socket.connect()
    }

    fun disconnect() {
        socket.close()
        broadcasterId = null
        accessToken = null
        userId = null
    }

    /** Asks for the moderator subscriptions again on the open socket, such as after the user became a moderator. */
    internal fun retryModeratorSubscriptions() {
        if (_moderating.value) return
        val session = socket.sessionId ?: return
        AppLog.i(TAG, "retry moderator subscriptions")
        subscribe(session, moderatorEventSubTypes())
    }

    fun close() {
        disconnect()
        reconnectExecutor.shutdownNow()
        statusQueue.close()
    }

    private fun onMessage(messageType: String?, text: String) {
        when (messageType) {
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
                    parseStreamStatusNotification(text)?.let { statusQueue.trySend(it) }
                }
            }
        }
    }

    private fun subscribe(sessionId: String, types: List<EventSubType>) {
        val id = broadcasterId ?: return
        val user = userId
        scope.launch {
            val token = eventSubToken(freshToken(), accessToken) ?: return@launch
            // The socket may have closed or reconnected while the token was read; its new welcome subscribes again.
            if (sessionId != socket.sessionId) return@launch
            accessToken = token
            types.forEach { type ->
                if (type.condition != EventSubCondition.Broadcaster && user == null) return@forEach
                launch { subscribeOnce(EventSubRequest(token, id, user, sessionId, type), attempt = 0) }
            }
        }
    }

    private fun subscribeOnce(request: EventSubRequest, attempt: Int) {
        val httpRequest = helixRequest(SUBSCRIBE_URL, request.accessToken, clientId)
            .post(eventSubSubscriptionBody(request).toString().toRequestBody(json))
            .build()
        try {
            http.newCall(httpRequest).execute().use { response ->
                val body = response.body.string()
                val outcome = eventSubSubscribeOutcome(
                    response.code, request.type.condition, request.sessionId, socket.sessionId, twitchAuthMessage(body),
                )
                onSubscribeAnswer(request, attempt, outcome, response.code, body)
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "subscribe failed ${request.type.name}: ${error.javaClass.simpleName}")
        }
    }

    private fun onSubscribeAnswer(
        request: EventSubRequest,
        attempt: Int,
        outcome: EventSubSubscribeOutcome,
        httpCode: Int,
        body: String,
    ) {
        val name = request.type.name
        when (outcome) {
            EventSubSubscribeOutcome.Subscribed -> {
                AppLog.i(TAG, "subscribed $name")
                if (request.type.condition == EventSubCondition.Moderator && request.sessionId == socket.sessionId) {
                    _moderating.value = true
                }
            }
            EventSubSubscribeOutcome.NotModerator -> AppLog.i(TAG, "not a moderator HTTP 403 $name")
            EventSubSubscribeOutcome.LoginRejected -> {
                AppLog.w(TAG, "subscribe rejected HTTP $httpCode $name")
                // 401 is an expired login; stream status needs no permission, so a 403 is unexpected.
                if (httpCode == HTTP_FORBIDDEN) reportSubscribeRejected(httpCode, name, body)
                disconnect()
            }
            EventSubSubscribeOutcome.Outdated ->
                AppLog.i(TAG, "subscribe outlived its session HTTP $httpCode $name")
            EventSubSubscribeOutcome.TransportLimit -> {
                val delaySeconds = eventSubTransportRetrySeconds(socket.keepaliveSeconds, attempt)
                AppLog.w(TAG, "subscribe hit the socket limit HTTP $httpCode $name, retry in ${delaySeconds}s")
                // The first refusal right after a reconnect is expected; a refused retry means live extra sockets.
                if (attempt > 0) reportSubscribeRejected(httpCode, name, body)
                scope.launch {
                    delay(delaySeconds.seconds)
                    if (request.sessionId == socket.sessionId) subscribeOnce(request, attempt + 1)
                }
            }
            EventSubSubscribeOutcome.Rejected -> {
                AppLog.w(TAG, "subscribe rejected HTTP $httpCode $name")
                reportSubscribeRejected(httpCode, name, body)
            }
            EventSubSubscribeOutcome.Failed -> AppLog.w(TAG, "subscribe failed HTTP $httpCode $name")
        }
    }

    private companion object {
        const val SOCKET_URL = "wss://eventsub.wss.twitch.tv/ws"
        const val SUBSCRIBE_URL = "https://api.twitch.tv/helix/eventsub/subscriptions"
    }
}
