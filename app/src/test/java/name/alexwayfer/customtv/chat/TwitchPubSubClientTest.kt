package name.alexwayfer.customtv.chat

import okhttp3.Request
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TwitchPubSubClientTest {
    private class FakeSocket(private val request: Request) : WebSocket {
        var cancelled = false
        val sent = mutableListOf<String>()
        override fun request(): Request = request
        override fun queueSize(): Long = 0
        override fun send(text: String): Boolean {
            sent += text
            return true
        }
        override fun send(bytes: ByteString): Boolean = true
        override fun close(code: Int, reason: String?): Boolean = true
        override fun cancel() {
            cancelled = true
        }
    }

    private class PlannedTask(val command: Runnable) : ScheduledFuture<Unit> {
        var cancelled = false
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            cancelled = true
            return true
        }
        override fun isCancelled(): Boolean = cancelled
        override fun isDone(): Boolean = cancelled
        override fun get() = Unit
        override fun get(timeout: Long, unit: TimeUnit) = Unit
        override fun getDelay(unit: TimeUnit): Long = 0
        override fun compareTo(other: Delayed): Int = 0
    }

    private class Scheduler : ScheduledThreadPoolExecutor(1) {
        val tasks = mutableListOf<PlannedTask>()
        override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> =
            PlannedTask(command).also { tasks += it }

        override fun scheduleWithFixedDelay(
            command: Runnable,
            initialDelay: Long,
            delay: Long,
            unit: TimeUnit,
        ): ScheduledFuture<*> = PlannedTask(command).also { tasks += it }

        fun runNext() {
            val task = tasks.first { !it.cancelled }
            tasks.remove(task)
            task.command.run()
        }
    }

    @Test
    fun anImmediateDnsFailureStillSchedulesTheNextConnection() {
        val scheduler = Scheduler()
        val sockets = mutableListOf<FakeSocket>()
        val factory = WebSocket.Factory { request, listener ->
            FakeSocket(request).also {
                sockets += it
                listener.onFailure(it, UnknownHostException(), null)
            }
        }
        val client = TwitchPubSubClient(factory, scheduler)
        try {
            client.listen("123", "test-token")
            assertEquals(1, scheduler.tasks.size)
            assertTrue(sockets.single().cancelled)
            scheduler.runNext()
            assertEquals(2, sockets.size)
            assertEquals(1, scheduler.tasks.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun anOldFailureCannotDisconnectTheReplacement() {
        val scheduler = Scheduler()
        val sockets = mutableListOf<Pair<FakeSocket, WebSocketListener>>()
        val factory = WebSocket.Factory { request, listener ->
            FakeSocket(request).also { sockets += it to listener }
        }
        val client = TwitchPubSubClient(factory, scheduler)
        try {
            client.listen("123")
            val (old, listener) = sockets.single()
            client.listen("456")
            listener.onFailure(old, UnknownHostException(), null)
            listener.onOpen(old, response(old))
            client.listen("456")
            assertEquals(2, sockets.size)
            assertTrue(old.cancelled)
            assertTrue(old.sent.isEmpty())
            assertTrue(scheduler.tasks.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun anImmediateOpenSubscribesAndStartsPinging() {
        val scheduler = Scheduler()
        val sockets = mutableListOf<FakeSocket>()
        val factory = WebSocket.Factory { request, listener ->
            FakeSocket(request).also {
                sockets += it
                listener.onOpen(it, response(it))
            }
        }
        val client = TwitchPubSubClient(factory, scheduler)
        try {
            client.listen("123", "test-token")
            val socket = sockets.single()
            assertTrue(socket.sent.single().contains("raid.123"))
            scheduler.runNext()
            assertEquals("""{"type":"PING"}""", socket.sent.last())
            client.listen("123", "test-token")
            assertEquals(1, sockets.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun disconnectPreventsAPlannedRetryFromReopening() {
        val scheduler = Scheduler()
        var opens = 0
        val factory = WebSocket.Factory { request, listener ->
            opens++
            FakeSocket(request).also { listener.onFailure(it, UnknownHostException(), null) }
        }
        val client = TwitchPubSubClient(factory, scheduler)
        try {
            client.listen("123")
            client.disconnect()
            scheduler.runNext()
            assertEquals(1, opens)
            assertTrue(scheduler.tasks.isEmpty())
        } finally {
            client.close()
        }
    }

    private fun response(socket: WebSocket): Response = Response.Builder()
        .request(socket.request())
        .protocol(Protocol.HTTP_1_1)
        .code(101)
        .message("")
        .build()
}
