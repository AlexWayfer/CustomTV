package name.alexwayfer.customtv.data

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EmoteHttpTest {
    @Test
    fun retryableStatusCodes() {
        assertTrue(EmoteHttp.isRetryableStatus(408))
        assertTrue(EmoteHttp.isRetryableStatus(429))
        assertTrue(EmoteHttp.isRetryableStatus(500))
        assertTrue(EmoteHttp.isRetryableStatus(503))
        assertFalse(EmoteHttp.isRetryableStatus(200))
        assertFalse(EmoteHttp.isRetryableStatus(404))
        assertFalse(EmoteHttp.isRetryableStatus(401))
    }

    @Test
    fun backoffGrowsAndCaps() {
        assertEquals(500.milliseconds, EmoteHttp.backoff(1))
        assertEquals(1.seconds, EmoteHttp.backoff(2))
        assertEquals(2.seconds, EmoteHttp.backoff(3))
        assertEquals(4.seconds, EmoteHttp.backoff(4))
        assertEquals(4.seconds, EmoteHttp.backoff(8))
    }

    @Test
    fun retriesRetryableFailuresThenSucceeds() = runBlocking {
        var attempts = 0
        val result = EmoteHttp.retry(attempts = 3, backoff = { Duration.ZERO }) {
            attempts++
            if (attempts < 3) {
                EmoteHttpBody.Failure(retryable = true)
            } else {
                EmoteHttpBody.Success("{}")
            }
        }
        assertEquals(3, attempts)
        assertEquals(EmoteHttpBody.Success("{}"), result)
    }

    @Test
    fun doesNotRetryFatalFailures() = runBlocking {
        var attempts = 0
        val result = EmoteHttp.retry(attempts = 3, backoff = { Duration.ZERO }) {
            attempts++
            EmoteHttpBody.Failure(retryable = false)
        }
        assertEquals(1, attempts)
        assertEquals(EmoteHttpBody.Failure(retryable = false), result)
    }

    @Test
    fun emptyStopsRetries() = runBlocking {
        var attempts = 0
        val result = EmoteHttp.retry(attempts = 3, backoff = { Duration.ZERO }) {
            attempts++
            EmoteHttpBody.Empty
        }
        assertEquals(1, attempts)
        assertEquals(EmoteHttpBody.Empty, result)
    }

    @Test
    fun classifyNotFoundAndRetryable() {
        assertEquals(EmoteHttpBody.Empty, EmoteHttp.classify(response(404, "missing")))
        assertEquals(
            EmoteHttpBody.Failure(retryable = true),
            EmoteHttp.classify(response(429, "slow down")),
        )
        assertEquals(
            EmoteHttpBody.Failure(retryable = false),
            EmoteHttp.classify(response(401, "nope")),
        )
        assertEquals(
            EmoteHttpBody.Success("""{"ok":true}"""),
            EmoteHttp.classify(response(200, """{"ok":true}""")),
        )
    }

    private fun response(code: Int, body: String): okhttp3.Response {
        return okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://example.com/emotes").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(code)
            .message("status")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
