package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Runs [task] after a delay in seconds; the result cancels it, and null means it cannot run any more. */
internal typealias EventSubSchedule = (delaySeconds: Long, task: () -> Unit) -> (() -> Unit)?

/** [EventSubSchedule] on [executor]: nothing runs once the executor is shut down. */
internal fun eventSubSchedule(executor: ScheduledExecutorService): EventSubSchedule = { delaySeconds, task ->
    if (executor.isShutdown) {
        null
    } else {
        try {
            val future = executor.schedule(task, delaySeconds, TimeUnit.SECONDS)
            ({ future.cancel(false) })
        } catch (_: RejectedExecutionException) {
            null
        }
    }
}

/**
 * What a socket tells its owner: a new session to subscribe on, every other message with its `message_type`, and the
 * socket being dropped, by the owner or for a reconnect.
 */
internal data class EventSubSocketEvents(
    val onWelcome: (sessionId: String) -> Unit,
    val onMessage: (messageType: String?, text: String) -> Unit,
    val onDisconnect: () -> Unit = {},
)

/**
 * One EventSub socket over its life: it opens, takes the session from the welcome, and reconnects when Twitch ends
 * the session, when the socket fails or closes, or when it stays silent past its keepalive. A socket it has already
 * replaced is not heard. The owner subscribes on each session and reads the other messages.
 *
 * Opening the socket and running a task later are functions, so unit tests run without the network or a clock.
 */
internal class EventSubSocket(
    private val tag: String,
    private val open: (WebSocketListener) -> WebSocket,
    private val schedule: EventSubSchedule,
    private val events: EventSubSocketEvents,
) {
    @Volatile
    private var webSocket: WebSocket? = null

    /** The session of the open socket, from its welcome; null while there is none. */
    @Volatile
    var sessionId: String? = null
        private set

    /** The keepalive timeout the last welcome named. */
    @Volatile
    var keepaliveSeconds: Long? = null
        private set

    @Volatile
    private var silenceCheck: (() -> Unit)? = null

    @Volatile
    private var reconnect: (() -> Unit)? = null

    @Volatile
    private var closedByUser = false

    /** Whether a socket is open or opening, and the owner has not closed it. */
    val connected: Boolean
        get() = webSocket != null && !closedByUser

    /** Opens a new socket in place of any current one. */
    fun connect() {
        closedByUser = false
        disconnectSocket()
        AppLog.i(tag, "connect")
        openSocket()
    }

    /** Closes the socket for good, until the next [connect]. */
    fun close() {
        closedByUser = true
        disconnectSocket()
    }

    private fun openSocket() {
        webSocket = open(Listener())
    }

    private fun disconnectSocket() {
        val current = webSocket
        webSocket = null
        sessionId = null
        events.onDisconnect()
        silenceCheck?.invoke()
        silenceCheck = null
        // A reconnect planned for the socket before stays away from the next one.
        reconnect?.invoke()
        reconnect = null
        current?.cancel()
    }

    /** Restarts the wait for the next message on [socket]: a socket silent past its keepalive timeout is dead. */
    private fun watchSilence(socket: WebSocket) {
        silenceCheck?.invoke()
        if (closedByUser) return
        val timeoutSeconds = eventSubSilenceTimeoutSeconds(keepaliveSeconds)
        silenceCheck = schedule(timeoutSeconds) {
            if (!closedByUser && webSocket == socket) {
                AppLog.w(tag, "silent for ${timeoutSeconds}s, reconnect")
                disconnectSocket()
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (closedByUser) return
        reconnect = schedule(EVENTSUB_RECONNECT_DELAY_SECONDS) {
            if (!closedByUser && webSocket == null) {
                AppLog.i(tag, "reconnect")
                openSocket()
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket != this@EventSubSocket.webSocket) return
            watchSilence(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@EventSubSocket.webSocket) return
            val welcome = eventSubSessionId(text)
            if (welcome != null) {
                AppLog.i(tag, "welcome")
                sessionId = welcome
                keepaliveSeconds = eventSubKeepaliveSeconds(text)
                watchSilence(webSocket)
                events.onWelcome(welcome)
                return
            }
            watchSilence(webSocket)
            val messageType = text.toJsonObjectOrNull()?.optJSONObject("metadata")?.optString("message_type")
            if (messageType == "session_reconnect") {
                AppLog.i(tag, "session ended")
                disconnectSocket()
                scheduleReconnect()
            } else {
                events.onMessage(messageType, text)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@EventSubSocket.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@EventSubSocket.webSocket) return
            this@EventSubSocket.webSocket = null
            sessionId = null
            AppLog.i(tag, "closed $code")
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@EventSubSocket.webSocket) return
            this@EventSubSocket.webSocket = null
            sessionId = null
            AppLog.w(tag, "failed: ${t.javaClass.simpleName}")
            scheduleReconnect()
        }
    }
}

/** How long a dropped socket waits before it opens again. */
internal const val EVENTSUB_RECONNECT_DELAY_SECONDS = 2L
