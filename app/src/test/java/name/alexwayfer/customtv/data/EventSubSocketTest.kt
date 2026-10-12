package name.alexwayfer.customtv.data

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EventSubSocketTest {
    private class Task(val delaySeconds: Long, val run: () -> Unit) {
        var cancelled = false
    }

    private class FakeSocket : WebSocket {
        var cancelled = false

        override fun request(): Request = Request.Builder().url("https://eventsub.test").build()
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
    private val welcomes = mutableListOf<String>()
    private val messages = mutableListOf<String?>()
    private var disconnects = 0

    private val socket = EventSubSocket(
        "EventSubSocketTest",
        open = { listener -> FakeSocket().also { opened += it to listener } },
        schedule = { delaySeconds, run ->
            val task = Task(delaySeconds, run)
            tasks += task
            ({ task.cancelled = true })
        },
        events = EventSubSocketEvents(
            onWelcome = { welcomes += it },
            onMessage = { messageType, _ -> messages += messageType },
            onDisconnect = { disconnects++ },
        ),
    )

    private val current get() = opened.last()

    /** Runs the tasks still waiting that were planned with [delaySeconds]. */
    private fun runPlanned(delaySeconds: Long) {
        val due = tasks.filter { !it.cancelled && it.delaySeconds == delaySeconds }
        tasks -= due.toSet()
        due.forEach { it.run() }
    }

    private fun welcome(sessionId: String, keepaliveSeconds: Long = 10) {
        val (ws, listener) = current
        listener.onMessage(
            ws,
            """{"metadata":{"message_type":"session_welcome"},""" +
                """"payload":{"session":{"id":"$sessionId","keepalive_timeout_seconds":$keepaliveSeconds}}}""",
        )
    }

    private fun message(type: String, ws: WebSocket = current.first) =
        current.second.onMessage(ws, """{"metadata":{"message_type":"$type"},"payload":{}}""")

    private fun response() = Response.Builder()
        .request(Request.Builder().url("https://eventsub.test").build())
        .protocol(Protocol.HTTP_1_1)
        .code(101)
        .message("")
        .build()

    @Test
    fun aWelcomeStartsTheSessionAndTheWaitForTheNextMessage() {
        socket.connect()
        welcome("s1", keepaliveSeconds = 30)

        assertEquals(listOf("s1"), welcomes)
        assertEquals("s1", socket.sessionId)
        assertEquals(30L, socket.keepaliveSeconds)
        assertEquals(listOf(35L), tasks.filter { !it.cancelled }.map { it.delaySeconds })
    }

    @Test
    fun eachMessageRestartsTheWaitAndOtherMessagesGoToTheOwner() {
        socket.connect()
        welcome("s1")
        message("notification")

        assertEquals(listOf("notification"), messages)
        assertEquals(1, tasks.count { !it.cancelled })
    }

    @Test
    fun aSocketSilentPastItsKeepaliveIsReplaced() {
        socket.connect()
        welcome("s1")
        val first = current.first

        runPlanned(15)
        assertTrue(first.cancelled)
        assertNull(socket.sessionId)
        assertEquals(1, opened.size)

        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(2, opened.size)
    }

    @Test
    fun twitchEndingTheSessionReconnectsAfterAPause() {
        socket.connect()
        welcome("s1")
        message("session_reconnect")

        assertNull(socket.sessionId)
        assertTrue(messages.isEmpty())
        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(2, opened.size)
    }

    @Test
    fun aFailedOrClosedSocketReconnects() {
        socket.connect()
        current.second.onFailure(current.first, IOException(), null)
        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(2, opened.size)

        current.second.onClosed(current.first, 1000, "")
        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(3, opened.size)
    }

    @Test
    fun aSocketAlreadyReplacedIsNotHeard() {
        socket.connect()
        val (old, oldListener) = current
        socket.connect()

        oldListener.onMessage(old, """{"metadata":{"message_type":"notification"},"payload":{}}""")
        oldListener.onFailure(old, IOException(), null)

        assertTrue(old.cancelled)
        assertTrue(messages.isEmpty())
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun aReconnectPlannedBeforeANewConnectDoesNotOpenASecondSocket() {
        socket.connect()
        current.second.onFailure(current.first, IOException(), null)
        socket.connect()
        assertTrue(tasks.single().cancelled)
        current.second.onFailure(current.first, IOException(), null)

        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(3, opened.size)
    }

    @Test
    fun aClosedSocketStaysClosed() {
        socket.connect()
        current.second.onOpen(current.first, response())
        socket.close()

        assertFalse(socket.connected)
        assertTrue(current.first.cancelled)
        assertTrue(tasks.all { it.cancelled })
        runPlanned(15)
        runPlanned(EVENTSUB_RECONNECT_DELAY_SECONDS)
        assertEquals(1, opened.size)
    }

    @Test
    fun theOwnerHearsEveryDrop() {
        socket.connect()
        welcome("s1")
        message("session_reconnect")
        socket.close()

        // The first connect drops nothing yet but tells the owner all the same, as a new login starts clean.
        assertEquals(3, disconnects)
    }
}
