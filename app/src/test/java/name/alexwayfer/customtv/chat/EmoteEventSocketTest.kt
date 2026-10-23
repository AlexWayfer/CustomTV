package name.alexwayfer.customtv.chat

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EmoteEventSocketTest {
    private class Task(val delaySeconds: Long, val run: () -> Unit)

    private class FakeSocket : WebSocket {
        var cancelled = false

        override fun request(): Request = Request.Builder().url("https://events.test").build()
        override fun queueSize(): Long = 0
        override fun send(text: String): Boolean = true
        override fun send(bytes: ByteString): Boolean = true
        override fun close(code: Int, reason: String?): Boolean = true
        override fun cancel() {
            cancelled = true
        }
    }

    private val tasks = mutableListOf<Task>()
    private val opened = mutableListOf<Pair<FakeSocket, WebSocketListener>>()
    private val subscribed = mutableListOf<String>()
    private val frames = mutableListOf<Pair<String, String>>()
    private var scheduling = true

    private val socket = EmoteEventSocket(
        open = { listener -> FakeSocket().also { opened += it to listener } },
        schedule = { delaySeconds, run ->
            if (scheduling) {
                val task = Task(delaySeconds, run)
                tasks += task
                ({ tasks -= task })
            } else {
                null
            }
        },
        onOpened = { _, target -> subscribed += target },
        onFrame = { _, target, text -> frames += target to text },
    )

    private val current get() = opened.last()

    private fun runPlanned() {
        val due = tasks.toList()
        tasks.clear()
        due.forEach { it.run() }
    }

    private fun response() = Response.Builder()
        .request(Request.Builder().url("https://events.test").build())
        .protocol(Protocol.HTTP_1_1)
        .code(101)
        .message("")
        .build()

    @Test
    fun anOpenedSocketSubscribesForTheTrimmedTargetAndPassesItsFrames() {
        socket.listen(" 123 ")
        current.second.onOpen(current.first, response())
        current.second.onMessage(current.first, "frame")

        assertEquals(listOf("123"), subscribed)
        assertEquals(listOf("123" to "frame"), frames)
    }

    @Test
    fun aBlankTargetOpensNothing() {
        socket.listen("  ")

        assertTrue(opened.isEmpty())
    }

    @Test
    fun theSameTargetKeepsTheOpenSocket() {
        socket.listen("123")
        socket.listen("123")

        assertEquals(1, opened.size)
    }

    @Test
    fun aNewTargetReplacesTheSocketAndTheOldOneIsNotHeard() {
        socket.listen("123")
        val (old, oldListener) = current
        socket.listen("456")

        oldListener.onMessage(old, "frame")
        oldListener.onFailure(old, IOException(), null)

        assertTrue(old.cancelled)
        assertEquals(2, opened.size)
        assertTrue(frames.isEmpty())
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun aFailedOrClosedSocketReopensAfterAPause() {
        socket.listen("123")
        current.second.onFailure(current.first, IOException(), null)

        assertEquals(EMOTE_EVENTS_RECONNECT_DELAY_SECONDS, tasks.single().delaySeconds)
        runPlanned()
        assertEquals(2, opened.size)

        current.second.onClosed(current.first, 1000, "")
        runPlanned()
        assertEquals(3, opened.size)
    }

    @Test
    fun aReopenPlannedBeforeANewTargetDoesNotOpenASecondSocket() {
        socket.listen("123")
        current.second.onFailure(current.first, IOException(), null)
        socket.listen("456")

        runPlanned()
        assertEquals(2, opened.size)
    }

    @Test
    fun aDisconnectedSocketStaysClosed() {
        socket.listen("123")
        current.second.onFailure(current.first, IOException(), null)
        socket.disconnect()

        runPlanned()
        assertEquals(1, opened.size)

        socket.listen("123")
        val second = current.first
        socket.disconnect()
        assertTrue(second.cancelled)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun aDisconnectCancelsTheReopenPlannedBefore() {
        socket.listen("123")
        current.second.onFailure(current.first, IOException(), null)
        socket.disconnect()

        assertTrue(tasks.isEmpty())
    }

    @Test
    fun aReconnectOpensANewSocketForTheSameTargetAtOnce() {
        socket.listen("123")
        val first = current.first
        socket.reconnect()
        current.second.onOpen(current.first, response())

        assertTrue(first.cancelled)
        assertEquals(2, opened.size)
        assertEquals(listOf("123"), subscribed)
    }

    @Test
    fun aReconnectAfterDisconnectOpensNothing() {
        socket.listen("123")
        socket.disconnect()
        socket.reconnect()

        assertEquals(1, opened.size)
    }

    @Test
    fun nothingReopensOnceTasksCannotRun() {
        scheduling = false
        socket.listen("123")
        current.second.onFailure(current.first, IOException(), null)

        assertEquals(1, opened.size)
        assertTrue(tasks.isEmpty())
    }
}
