package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class TwitchIrcClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Bounds the TLS handshake and the upgrade answer; the open socket is watched by the ping.
        .readTimeout(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {
    private val _messages = MutableSharedFlow<ChatMessage>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<ChatMessage> = _messages

    private val _state = MutableStateFlow(ChatConnectionState.Disconnected)
    val state: StateFlow<ChatConnectionState> = _state

    private val _roomModes = MutableStateFlow(ChatRoomModes())
    internal val roomModes: StateFlow<ChatRoomModes> = _roomModes

    private var webSocket: WebSocket? = null
    private var channel: String? = null
    @Volatile
    private var closedByUser = false

    fun connect(channelLogin: String) {
        val normalized = channelLogin.lowercase().removePrefix("#")
        AppLog.i(TAG, "connect $normalized")
        closedByUser = false
        val sameChannel = channel == normalized
        disconnectSocket()
        channel = normalized
        _roomModes.value = roomModesAfterConnect(_roomModes.value, sameChannel)
        _state.value = ChatConnectionState.Connecting
        openSocket()
    }

    fun pause() {
        AppLog.i(TAG, "pause")
        closedByUser = true
        disconnectSocket()
        _state.value = ChatConnectionState.Disconnected
    }

    fun disconnect() {
        AppLog.i(TAG, "disconnect")
        closedByUser = true
        channel = null
        disconnectSocket()
        _roomModes.value = ChatRoomModes()
        _state.value = ChatConnectionState.Disconnected
    }

    private fun openSocket() {
        val request = Request.Builder()
            .url(IRC_URL)
            .build()
        webSocket = client.newWebSocket(request, SocketListener())
    }

    private fun disconnectSocket() {
        val current = webSocket
        webSocket = null
        current?.cancel()
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            AppLog.i(TAG, "socket open")
            val nick = "justinfan${Random.nextInt(10_000, 99_999)}"
            webSocket.send("CAP REQ :twitch.tv/tags twitch.tv/commands")
            webSocket.send("PASS SCHMOOPIIE")
            webSocket.send("NICK $nick")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != this@TwitchIrcClient.webSocket) return
            text.split("\r\n").forEach { line ->
                if (line.isBlank()) return@forEach
                handleLine(webSocket, line)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@TwitchIrcClient.webSocket) return
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != this@TwitchIrcClient.webSocket) return
            AppLog.i(TAG, "socket closed code=$code reason=$reason")
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != this@TwitchIrcClient.webSocket) return
            AppLog.w(TAG, "socket failed", t)
            handleDisconnect()
        }
    }

    private fun handleLine(webSocket: WebSocket, line: String) {
        if (line.startsWith("PING")) {
            val payload = line.substringAfter("PING").trim()
            webSocket.send(if (payload.isEmpty()) "PONG" else "PONG $payload")
            return
        }
        val parsed = IrcMessageParser.parseLine(line) ?: return
        when (parsed.command) {
            "001" -> {
                val current = channel ?: return
                AppLog.i(TAG, "registered, joining #$current")
                webSocket.send("JOIN #$current")
                _state.value = ChatConnectionState.Connected
            }
            "PRIVMSG" -> {
                IrcMessageParser.parsePrivMsg(line)?.let { _messages.tryEmit(it) }
            }
            "USERNOTICE" -> {
                IrcMessageParser.parseUserNotice(line)?.let { _messages.tryEmit(it) }
            }
            "NOTICE" -> {
                IrcMessageParser.parseNotice(line)?.let { _messages.tryEmit(it) }
            }
            "CLEARMSG" -> {
                IrcMessageParser.parseClearMsg(line)?.let { _messages.tryEmit(it) }
            }
            "CLEARCHAT" -> {
                IrcMessageParser.parseClearChat(line)?.let { _messages.tryEmit(it) }
            }
            "ROOMSTATE" -> {
                _roomModes.value = chatRoomModesAfter(_roomModes.value, parsed.tags)
            }
            "RECONNECT" -> {
                AppLog.i(TAG, "server requested reconnect")
                _state.value = ChatConnectionState.Reconnecting
            }
        }
    }

    private fun handleDisconnect() {
        if (closedByUser) {
            AppLog.i(TAG, "closed")
            _state.value = ChatConnectionState.Disconnected
            return
        }
        if (channel != null) {
            AppLog.i(TAG, "reconnecting")
            _state.value = ChatConnectionState.Reconnecting
        } else {
            AppLog.i(TAG, "disconnected")
            _state.value = ChatConnectionState.Disconnected
        }
    }

    private companion object {
        const val TAG = "ChatIrc"
        const val SOCKET_TIMEOUT_SECONDS = 10L
        const val IRC_URL = "wss://irc-ws.chat.twitch.tv:443"
    }
}
