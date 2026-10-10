package name.alexwayfer.customtv.auth

import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeviceApprovalTest {
    private val tokens = TwitchTokens(accessToken = "access", refreshToken = "refresh", expiresInSeconds = 3600)
    private val login = TwitchDeviceLogin(
        deviceCode = "device",
        userCode = "ABCDEFGH",
        verificationUri = "https://www.twitch.tv/activate",
        expiresInSeconds = 30,
        intervalSeconds = 5,
    )

    private class Clock {
        var now = 0L
        val waits = mutableListOf<Long>()
    }

    private fun approval(clock: Clock, vararg answers: () -> DeviceGrant): DeviceApproval {
        val queue = ArrayDeque(answers.toList())
        return runBlocking {
            awaitDeviceApproval(
                login = login,
                poll = { queue.removeFirstOrNull()?.invoke() ?: DeviceGrant.Pending },
                log = {},
                nowMillis = { clock.now },
                wait = { millis ->
                    clock.waits += millis
                    clock.now += millis
                },
            )
        }
    }

    @Test
    fun pendingThenApprovedReturnsTokensAfterFirstPollThenInterval() {
        val clock = Clock()
        val result = approval(clock, { DeviceGrant.Pending }, { DeviceGrant.Approved(tokens) })
        assertEquals(DeviceApproval.Approved(tokens), result)
        assertEquals(listOf(1_000L, 5_000L), clock.waits)
    }

    @Test
    fun deniedFails() {
        assertEquals(DeviceApproval.Failed, approval(Clock(), { DeviceGrant.Denied }))
    }

    @Test
    fun unreadableGrantFails() {
        assertEquals(DeviceApproval.Failed, approval(Clock(), { DeviceGrant.Failed }))
    }

    @Test
    fun expiredGrantTimesOut() {
        assertEquals(DeviceApproval.TimedOut, approval(Clock(), { DeviceGrant.Expired }))
    }

    @Test
    fun networkErrorAndServerErrorKeepPolling() {
        val result = approval(
            Clock(),
            { throw SocketTimeoutException() },
            { DeviceGrant.Unavailable(503) },
            { DeviceGrant.SlowDown },
            { DeviceGrant.Approved(tokens) },
        )
        assertEquals(DeviceApproval.Approved(tokens), result)
    }

    @Test
    fun networkErrorDoesNotCountAsCompletedPoll() {
        val clock = Clock()
        approval(clock, { throw SocketTimeoutException() }, { DeviceGrant.Approved(tokens) })
        assertEquals(listOf(1_000L, 1_000L), clock.waits)
    }

    @Test
    fun rejectedRequestIsThrown() {
        assertThrows(TwitchAuthException::class.java) {
            approval(Clock(), { throw TwitchAuthException(400) })
        }
    }

    @Test
    fun codeExpiringWhileWaitingTimesOutWithoutAnotherPoll() {
        val clock = Clock()
        var polls = 0
        val result = approval(clock, *Array(10) { { polls += 1; DeviceGrant.Pending } })
        assertEquals(DeviceApproval.TimedOut, result)
        // Polls at 1 s, 6 s, ..., 26 s; the wait that ends at 31 s passes the 30 s deadline.
        assertEquals(6, polls)
    }
}
