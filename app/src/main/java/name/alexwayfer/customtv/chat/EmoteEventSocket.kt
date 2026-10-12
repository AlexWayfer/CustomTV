package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.EventSubSchedule
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * One socket of a third-party emote service over its life: it listens for one target, such as a channel or an emote
 * set, reopens a while after it fails or closes, and does not hear a socket it has already replaced. The owner sends
 * its subscription when the socket opens or when a frame asks for it, and reads the frames.
 *
 * Opening the socket and running a task later are functions, so unit tests run without the network or a clock.
 */
internal class EmoteEventSocket(
    private val open: (WebSocketListener) -> WebSocket,
    private val schedule: EventSubSchedule,
    private val onOpened: (socket: WebSocket, target: String) -> Unit = { _, _ -> },
    private val onFrame: (socket: WebSocket, target: String, text: String) -> Unit,
) {
    private var webSocket: WebSocket? = null
    private var target: String? = null

    @Volatile
    private var closedByUser = false

    /** Listens for [target]; the same target on an open socket keeps it, a blank one is ignored. */
    fun listen(target: String) {
        val normalized = target.trim()
        if (normalized.isEmpty()) return
        if (this.target == normalized && webSocket != null && !closedByUser) return
        closedByUser = false
        this.target = normalized
        disconnectSocket()
        openSocket()
    }

    /** Closes the socket for good, until the next [listen]. */
    fun disconnect() {
        closedByUser = true
        target = null
        disconnectSocket()
    }

    /** Opens a new socket for the same target at once, as the service asks before it goes away. */
    fun reconnect() {
        if (closedByUser) return
        val id = target ?: return
        disconnectSocket()
        target = id
        openSocket()
    }

    private fun openSocket() {
        webSocket = open(Listener())
    }

    private fun disconnectSocket() {
        val current = webSocket
        webSocket = null
        current?.cancel()
    }

    private fun handleDisconnect() {
        if (closedByUser) return
        val id = target ?: return
        webSocket = null
        schedule(EMOTE_EVENTS_RECONNECT_DELAY_SECONDS) {
            if (!closedByUser && target == id && webSocket == null) {
                openSocket()
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val id = target ?: return
            onOpened(webSocket, id)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@EmoteEventSocket.webSocket) return
            val id = target ?: return
            onFrame(webSocket, id, text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@EmoteEventSocket.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@EmoteEventSocket.webSocket) return
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@EmoteEventSocket.webSocket) return
            handleDisconnect()
        }
    }
}

/** How long a dropped emote events socket waits before it opens again. */
internal const val EMOTE_EVENTS_RECONNECT_DELAY_SECONDS = 2L
