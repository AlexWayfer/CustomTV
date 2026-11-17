package name.alexwayfer.customtv.player

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayerStylesheetFailureTest {
    @Test
    fun stylesheetHttpFailureLogsItsCodeWithoutTheRequestQuery() {
        val response = WebResourceResponse("text/css", "UTF-8", 503, "Unavailable", emptyMap(), null)
        client().onReceivedHttpError(view(), request(STYLESHEET + "?private=secret"), response)
        assertEquals("Player stylesheet failed, HTTP 503", ShadowLog.getLogsForTag(PLAYER_LOG_TAG).lastOrNull()?.msg)
    }

    @Test
    fun unrelatedSubresourceFailuresAreNotReportedAsStylesheetFailures() {
        val response = WebResourceResponse("text/css", "UTF-8", 503, "Unavailable", emptyMap(), null)
        for (url in listOf(
            "https://example.com/assets/player-main-123.css",
            "https://assets.twitch.tv/assets/other.css",
            "https://assets.twitch.tv/assets/player-main-123.js",
        )) {
            client().onReceivedHttpError(view(), request(url), response)
        }
        assertTrue(ShadowLog.getLogsForTag(PLAYER_LOG_TAG).none { it.msg.startsWith("Player stylesheet failed") })
    }

    private fun client() = PlayerNavigationClient(
        Handler(Looper.getMainLooper()),
        isReleased = { false },
        mediaNetworkPage = { "page" },
        events = PlayerPageEvents(
            hideTopOverlay = {},
            onPlayerPageStarted = {},
            onPlayerPageLoaded = {},
            onPlayerPageFailed = { throw AssertionError("Subresource failure must not reload playback: $it") },
            restorePlayer = { throw AssertionError("Playback must not reload") },
            playlistArrival = { {} },
        ),
    )

    private fun view() = WebView(RuntimeEnvironment.getApplication())

    private fun request(url: String) = object : WebResourceRequest {
        override fun getUrl(): Uri = url.toUri()
        override fun isForMainFrame(): Boolean = false
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    private companion object {
        const val STYLESHEET = "https://assets.twitch.tv/assets/player-main-123.css"
    }
}
