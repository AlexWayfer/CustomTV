package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadResultTest {
    @Test
    fun cancellationLeavesTheLoad() {
        var escaped = false
        try {
            resultUnlessCancelled { throw CancellationException("stop") }
        } catch (_: CancellationException) {
            escaped = true
        }
        assertTrue(escaped)
    }

    @Test
    fun otherFailureBecomesAResult() {
        val result = resultUnlessCancelled { error("nope") }
        assertTrue(result.isFailure)
    }

    @Test
    fun successKeepsANullValue() {
        val result = resultUnlessCancelled<String?> { null }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
