package name.alexwayfer.customtv.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketTasksTest {
    @Test
    fun openSocketAcceptsReconnectAndPing() {
        assertTrue(socketTaskAllowed(closedByUser = false, executorShutdown = false))
    }

    @Test
    fun userDisconnectDoesNotScheduleAnotherSocket() {
        assertFalse(socketTaskAllowed(closedByUser = true, executorShutdown = false))
    }

    @Test
    fun closedExecutorDoesNotScheduleAnotherSocket() {
        assertFalse(socketTaskAllowed(closedByUser = false, executorShutdown = true))
    }
}
