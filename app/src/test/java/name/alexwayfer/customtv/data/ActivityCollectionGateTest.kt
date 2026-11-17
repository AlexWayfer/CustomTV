package name.alexwayfer.customtv.data

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ActivityCollectionGateTest {
    @Test
    fun optingOutCancelsTheRunningRequestAndOptingInRestartsIt() = runBlocking {
        val enabled = MutableStateFlow(false)
        val access = MutableStateFlow<ActivityAuthorization?>(ActivityAuthorization("free"))
        val events = Channel<String>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            collectActivityStatistics(enabled, access) {
                events.send("started")
                try { awaitCancellation() } finally { events.trySend("stopped") }
            }
        }
        try {
            assertTrue(events.tryReceive().isFailure)
            enabled.value = true
            assertEquals("started", withTimeout(5.seconds) { events.receive() })
            enabled.value = false
            assertEquals("stopped", withTimeout(5.seconds) { events.receive() })
            enabled.value = true
            assertEquals("started", withTimeout(5.seconds) { events.receive() })
        } finally {
            job.cancel()
            job.join()
        }
    }

    @Test
    fun premiumLogoutCancelsReportingUntilAnotherLoginArrives() = runBlocking {
        val enabled = MutableStateFlow(true)
        val access = MutableStateFlow<ActivityAuthorization?>(null)
        val events = Channel<String>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            collectActivityStatistics(enabled, access) { authorization ->
                events.send(authorization.deviceSecret.orEmpty())
                try { awaitCancellation() } finally { events.trySend("stopped") }
            }
        }
        try {
            access.value = ActivityAuthorization("premium", "first")
            assertEquals("first", withTimeout(5.seconds) { events.receive() })
            access.value = null
            assertEquals("stopped", withTimeout(5.seconds) { events.receive() })
            access.value = ActivityAuthorization("premium", "second")
            assertEquals("second", withTimeout(5.seconds) { events.receive() })
        } finally {
            job.cancel()
            job.join()
        }
    }
}
