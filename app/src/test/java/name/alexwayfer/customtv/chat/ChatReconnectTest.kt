package name.alexwayfer.customtv.chat

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReconnectTest {
    private var channel: String? = "quin69"
    private val connects = mutableListOf<String>()

    /** The pause before a reconnect ends when the test sends here. */
    private val pauseEnds = Channel<Unit>(Channel.UNLIMITED)

    private fun withReconnect(block: suspend (ChatReconnect) -> Unit) = runBlocking {
        val reconnect = ChatReconnect(
            this,
            currentChannel = { channel },
            connect = { connects += it },
            wait = { pauseEnds.receive() },
        )
        try {
            block(reconnect)
        } finally {
            reconnect.cancel()
        }
    }

    @Test
    fun aLostConnectionConnectsAgainAfterThePause() = withReconnect { reconnect ->
        reconnect.schedule()
        yield()
        assertTrue(connects.isEmpty())

        pauseEnds.send(Unit)
        yield()
        assertEquals(listOf("quin69"), connects)
    }

    @Test
    fun aChannelOpenedDuringThePauseIsNotReconnectedToTheOldOne() = withReconnect { reconnect ->
        reconnect.schedule()
        yield()
        channel = "xqc"
        pauseEnds.send(Unit)
        yield()

        assertTrue(connects.isEmpty())
    }

    @Test
    fun aNewConnectCancelsTheWaitingReconnect() = withReconnect { reconnect ->
        reconnect.schedule()
        yield()
        reconnect.cancel()
        pauseEnds.send(Unit)
        yield()

        assertTrue(connects.isEmpty())
    }

    @Test
    fun onlyTheLatestReconnectWaits() = withReconnect { reconnect ->
        reconnect.schedule()
        yield()
        reconnect.schedule()
        yield()
        pauseEnds.send(Unit)
        pauseEnds.send(Unit)
        yield()

        assertEquals(listOf("quin69"), connects)
    }

    @Test
    fun withoutAChannelNothingWaits() = withReconnect { reconnect ->
        channel = null
        reconnect.schedule()
        pauseEnds.send(Unit)
        yield()

        assertTrue(connects.isEmpty())
    }
}
