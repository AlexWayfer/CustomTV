package name.alexwayfer.customtv.chat

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal class FakeIrcSocket(
    private val request: Request,
    private val listener: WebSocketListener,
) : WebSocket {
    val sent = mutableListOf<String>()
    var cancelled = false

    override fun request(): Request = request
    override fun queueSize(): Long = 0
    override fun send(text: String): Boolean = sent.add(text)
    override fun send(bytes: ByteString): Boolean = false
    override fun close(code: Int, reason: String?): Boolean = true
    override fun cancel() {
        cancelled = true
    }

    fun open() {
        val upgraded = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(101).message("").build()
        listener.onOpen(this, upgraded)
    }

    fun receive(line: String) {
        listener.onMessage(this, line + "\r\n")
    }

    fun register() {
        open()
        receive(":tmi.twitch.tv 001 viewer :Welcome")
    }
}
