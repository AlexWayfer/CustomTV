package name.alexwayfer.customtv.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PollFrameLogTest {
    private fun logged(type: String, pollId: String?, nowMillis: Long, lastPollId: String?, lastAtMillis: Long) =
        pollFrameLogged(type, pollId, nowMillis, lastPollId, lastAtMillis)

    @Test
    fun theFirstUpdateOfAPollIsLogged() {
        assertTrue(logged(POLL_UPDATE, "p1", nowMillis = 1_000, lastPollId = null, lastAtMillis = 0))
    }

    @Test
    fun anotherUpdateOfThatPollSoonAfterIsNot() {
        assertFalse(logged(POLL_UPDATE, "p1", nowMillis = 5_000, lastPollId = "p1", lastAtMillis = 1_000))
    }

    @Test
    fun anUpdateOfThatPollAfterTheIntervalIsLogged() {
        assertTrue(
            logged(POLL_UPDATE, "p1", nowMillis = 1_000 + POLL_UPDATE_LOG_INTERVAL_MILLIS, lastPollId = "p1", lastAtMillis = 1_000),
        )
    }

    @Test
    fun theFirstUpdateOfTheNextPollIsLogged() {
        assertTrue(logged(POLL_UPDATE, "p2", nowMillis = 2_000, lastPollId = "p1", lastAtMillis = 1_000))
    }

    @Test
    fun otherFramesAreAlwaysLogged() {
        assertTrue(logged("POLL_COMPLETE", "p1", nowMillis = 2_000, lastPollId = "p1", lastAtMillis = 1_000))
    }

    @Test
    fun anUnreadableUpdateIsLogged() {
        assertTrue(logged(POLL_UPDATE, null, nowMillis = 2_000, lastPollId = "p1", lastAtMillis = 1_000))
    }
}
