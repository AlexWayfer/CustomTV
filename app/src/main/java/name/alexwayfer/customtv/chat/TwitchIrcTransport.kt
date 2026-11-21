package name.alexwayfer.customtv.chat

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import kotlin.time.Duration.Companion.seconds

internal val twitchIrcSocketTimeout = 10.seconds
internal const val TWITCH_IRC_CAPABILITIES = "CAP REQ :twitch.tv/tags twitch.tv/commands"

internal val twitchIrcSocketRequest: Request = Request.Builder().url("wss://irc-ws.chat.twitch.tv:443").build()

internal val twitchIrcSocketFactory: WebSocket.Factory by lazy {
    OkHttpClient.Builder()
        .pingInterval(twitchIrcSocketTimeout)
        // The read timeout bounds the upgrade. WebSocket pings watch the open socket.
        .readTimeout(twitchIrcSocketTimeout)
        .build()
}

internal fun ircPong(line: String): String? {
    if (!line.startsWith("PING")) return null
    val payload = line.substringAfter("PING").trim()
    return if (payload.isEmpty()) "PONG" else "PONG $payload"
}
