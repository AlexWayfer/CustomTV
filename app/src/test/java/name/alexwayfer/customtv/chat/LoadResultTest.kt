package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CancellationException
import org.json.JSONException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LoadResultTest {
    @Test
    fun cancellationLeavesTheLoad() {
        var escaped = false
        try {
            loadResult { throw CancellationException("stop") }
        } catch (_: CancellationException) {
            escaped = true
        }
        assertTrue(escaped)
    }

    @Test
    fun networkFailureBecomesAResult() {
        val result = loadResult { throw IOException("offline") }
        assertTrue(result.isFailure)
    }

    @Test
    fun unreadableAnswerBecomesAResult() {
        val result = loadResult { throw JSONException("not json") }
        assertTrue(result.isFailure)
    }

    @Test
    fun bugLeavesTheLoad() {
        var escaped = false
        try {
            loadResult { error("bug") }
        } catch (_: IllegalStateException) {
            escaped = true
        }
        assertTrue(escaped)
    }

    @Test
    fun successKeepsANullValue() {
        val result = loadResult<String?> { null }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
