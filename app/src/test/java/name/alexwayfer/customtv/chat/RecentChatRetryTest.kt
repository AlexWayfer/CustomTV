package name.alexwayfer.customtv.chat

import org.json.JSONException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InterruptedIOException

class RecentChatRetryTest {
    @Test
    fun aTimeoutIsRetried() {
        assertTrue(recentChatRetryable(null, InterruptedIOException("timeout")))
    }

    @Test
    fun aServerErrorIsRetried() {
        assertTrue(recentChatRetryable(500, null))
        assertTrue(recentChatRetryable(503, null))
    }

    @Test
    fun anUnknownChannelIsNotRetried() {
        assertFalse(recentChatRetryable(404, null))
        assertFalse(recentChatRetryable(400, null))
    }

    @Test
    fun aMalformedAnswerIsNotRetried() {
        assertFalse(recentChatRetryable(null, JSONException("bad")))
    }
}
