package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.data.HTTP_UNAUTHORIZED
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * The authenticated write connection. It can send to the channel without joining it, so USERSTATE
 * on this socket is an acknowledgment of a send. The separate read connection receives the actual chat row.
 */
internal class TwitchIrcSender(
    channelLogin: String,
    userLogin: String,
    private val client: WebSocket.Factory = twitchIrcSocketFactory,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val room = ircLogin(channelLogin)?.let { "#$it" }
    private val nick = ircLogin(userLogin)
    private var connection: Connection? = null

    suspend fun send(accessToken: String, message: String, replyParentId: String?): ChatSendResult {
        currentCoroutineContext().ensureActive()
        if (nick == null || !validIrcAccessToken(accessToken)) {
            return ChatSendResult.Rejected(HTTP_UNAUTHORIZED, "")
        }
        val command = room?.let { ircChatMessage(it, message, replyParentId) }
        val send = command?.let { enqueue(accessToken, it) }
        return if (send == null) {
            ChatSendResult.Unavailable
        } else {
            try {
                withTimeoutOrNull(twitchIrcSocketTimeout) { send.result.await() } ?: ChatSendResult.Unavailable
            } finally {
                abandon(send)
            }
        }
    }

    @Synchronized
    fun disconnect() {
        connection?.let { retire(it) }
    }

    @Synchronized
    private fun enqueue(token: String, command: String): PendingSend? {
        val current = connection
        if (current?.pending != null) return null
        val send = PendingSend(command)
        if (current?.token == token) {
            current.pending = send
            current.sendPending()
        } else {
            current?.let { retire(it) }
            val opening = Connection(token, send)
            connection = opening
            val socket = client.newWebSocket(twitchIrcSocketRequest, opening)
            if (connection === opening) opening.socket = socket else socket.cancel()
        }
        return send
    }

    @Synchronized
    private fun abandon(send: PendingSend) {
        val current = connection ?: return
        // A late untagged refusal on an abandoned socket cannot become the next send's result.
        if (current.pending === send) retire(current)
    }

    private fun retire(closing: Connection, result: ChatSendResult = ChatSendResult.Unavailable) {
        connection = null
        closing.complete(result)
        closing.socket?.cancel()
    }

    private class PendingSend(val command: String) {
        val result = CompletableDeferred<ChatSendResult>()
        var written = false
    }

    private inner class Connection(val token: String, var pending: PendingSend?) : WebSocketListener() {
        var socket: WebSocket? = null
        private var registered = false

        override fun onOpen(webSocket: WebSocket, response: Response) {
            withCurrent {
                socket = webSocket
                if (!webSocket.send(TWITCH_IRC_CAPABILITIES) ||
                    !webSocket.send("PASS oauth:$token") ||
                    !webSocket.send("NICK $nick")
                ) {
                    retire(this)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            withCurrent {
                for (line in text.lineSequence()) {
                    if (connection !== this) break
                    handleLine(webSocket, line)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            withCurrent { retire(this) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            withCurrent { retire(this) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            withCurrent {
                AppLog.w(TAG, "socket failed: ${t.javaClass.simpleName}")
                retire(this)
            }
        }

        private fun withCurrent(action: () -> Unit) {
            synchronized(this@TwitchIrcSender) {
                if (connection === this) action()
            }
        }

        private fun handleLine(webSocket: WebSocket, raw: String) {
            ircPong(raw)?.let { pong ->
                if (!webSocket.send(pong)) retire(this)
                return
            }
            val line = IrcLineParser.parse(raw) ?: return
            when {
                line.command == "RECONNECT" -> retire(this)
                !registered -> handleRegistration(line)
                pending?.written == true && line.params.firstOrNull() == room ->
                    ircChatSendResult(line, nowMillis())?.let(::complete)
            }
        }

        private fun handleRegistration(line: IrcLine) {
            when {
                ircLoginRejected(line) -> retire(this, ChatSendResult.Rejected(HTTP_UNAUTHORIZED, ""))
                line.command == "CAP" && "NAK" in line.params -> retire(this)
                line.command == "001" -> {
                    registered = true
                    sendPending()
                }
            }
        }

        fun sendPending() {
            val send = pending ?: return
            if (!registered || send.written) return
            send.written = true
            if (socket?.send(send.command) != true) retire(this)
        }

        fun complete(result: ChatSendResult) {
            val send = pending ?: return
            pending = null
            if (result is ChatSendResult.Dropped) AppLog.w(TAG, "chat send dropped ${result.code}")
            send.result.complete(result)
        }
    }

    private companion object {
        const val TAG = "ChatIrcSend"
    }
}
