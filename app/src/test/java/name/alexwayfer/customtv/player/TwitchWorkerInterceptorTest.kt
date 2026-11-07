package name.alexwayfer.customtv.player

import android.app.Application
import android.content.res.AssetManager
import android.net.Uri
import android.webkit.WebResourceRequest
import java.io.IOException
import name.alexwayfer.customtv.diagnostics.AppLog
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TwitchWorkerInterceptorTest {
    private val assets: AssetManager get() = RuntimeEnvironment.getApplication().assets

    @Test
    fun patchesTheWorkerAndCachesTheCorrectedBodyForLaterPageLoads() {
        var downloads = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            downloads += 1
            assertNull(chain.request().header("If-None-Match"))
            assertNull(chain.request().header("Range"))
            response(chain.request(), TwitchWorkerScriptTest.REMOVE)
        }.build()
        val interceptor = TwitchWorkerInterceptor(http)
        val headers = mapOf("If-None-Match" to "old", "Range" to "bytes=10-20")
        val answer = interceptor.intercept(request(headers = headers), assets)!!
        assertEquals(200, answer.statusCode)
        assertEquals("application/javascript", answer.mimeType)
        assertEquals("utf-8", answer.encoding)
        assertEquals("no-store", answer.responseHeaders["Cache-Control"])
        val origin = answer.responseHeaders.entries.firstOrNull {
            it.key.equals("Access-Control-Allow-Origin", ignoreCase = true)
        }?.value
        assertEquals("*", origin)
        assertFalse(answer.responseHeaders.keys.any { it.equals("Content-Length", ignoreCase = true) })
        assertFalse(answer.responseHeaders.keys.any { it.equals("ETag", ignoreCase = true) })
        assertFalse(answer.responseHeaders.keys.any { it.equals("Transfer-Encoding", ignoreCase = true) })
        val script = answer.data.bufferedReader().use { it.readText() }
        val cleanup = assets.open("player/worker-buffer-cleanup.js").bufferedReader().use { it.readText().trim() }
        assertEquals(cleanup, script)
        val again = interceptor.intercept(request(), assets)!!
        assertEquals(script, again.data.bufferedReader().use { it.readText() })
        assertEquals(1, downloads)
        assertTrue(AppLog.snapshot().any { it.contains("Worker buffer cleanup applied") })
    }

    @Test
    fun leavesOtherRequestsToTheWebView() {
        var downloads = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            downloads += 1
            response(chain.request(), "unused")
        }.build()
        val interceptor = TwitchWorkerInterceptor(http)
        assertNull(interceptor.intercept(request(url = "https://example.com/player.js"), assets))
        assertNull(interceptor.intercept(request(method = "POST"), assets))
        assertEquals(0, downloads)
    }

    @Test
    fun servesAnUnknownSdkUnchangedAndReportsThatTheFixIsMissing() {
        val original = "new SDK format"
        val http = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), original) }.build()
        val answer = TwitchWorkerInterceptor(http).intercept(request(), assets)!!
        assertEquals(original, answer.data.bufferedReader().use { it.readText() })
        assertTrue(AppLog.snapshot().any { it.contains("Worker buffer cleanup not applied") })
    }

    @Test
    fun fallsBackToTheWebViewAfterNetworkOrUnusableHttpAnswers() {
        val failed = OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build()
        assertNull(TwitchWorkerInterceptor(failed).intercept(request(), assets))
        for (status in listOf(304, 404, 500)) {
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                response(chain.request(), "unusable", status)
            }.build()
            assertNull(TwitchWorkerInterceptor(http).intercept(request(), assets))
        }
    }

    @Test
    fun refusesAnUnexpectedlyLargeScript() {
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            response(chain.request(), "x".repeat(2 * 1024 * 1024))
        }.build()
        assertNull(TwitchWorkerInterceptor(http).intercept(request(), assets))
    }

    private fun request(
        url: String = WORKER_URL,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
    ): WebResourceRequest = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun getMethod(): String = method
        override fun getRequestHeaders(): Map<String, String> = headers
        override fun isForMainFrame(): Boolean = false
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
    }

    private fun response(request: okhttp3.Request, body: String, code: Int = 200): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("OK")
        .header("Content-Type", "application/javascript")
        .header("Content-Length", body.length.toString())
        .header("Transfer-Encoding", "chunked")
        .header("ETag", "original")
        .header("Access-Control-Allow-Origin", "*")
        .body(body.toResponseBody())
        .build()

    private companion object {
        const val WORKER_URL = "https://assets.twitch.tv/assets/amazon-ivs-wasmworker.min-1da59c3161bea7d07190.js"
    }
}
