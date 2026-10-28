package name.alexwayfer.customtv.player

import android.graphics.Bitmap
import android.os.Handler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import name.alexwayfer.customtv.data.HTTP_SERVER_ERROR
import name.alexwayfer.customtv.diagnostics.AppLog

/** What the player does as its page loads, fails, or navigates away. */
internal data class PlayerPageEvents(
    val hideTopOverlay: () -> Unit,
    val onPlayerPageStarted: () -> Unit,
    val onPlayerPageLoaded: () -> Unit,
    val onPlayerPageFailed: (reason: String) -> Unit,
    val restorePlayer: () -> Unit,
    val playlistArrival: () -> () -> Unit,
)

internal class PlayerNavigationClient(
    private val handler: Handler,
    private val isReleased: () -> Boolean,
    private val events: PlayerPageEvents,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = shouldBlockPlayerNavigation(
        request.url,
        request.isForMainFrame,
    )

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        if (isReleased()) null else UsherAudioOnly.intercept(request, events.playlistArrival)

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (isReleased() || !isPlayerDocumentUrl(url)) return
        events.onPlayerPageStarted()
        events.hideTopOverlay()
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        if (isReleased() || !isPlayerDocumentUrl(url)) return
        events.hideTopOverlay()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (isReleased() || !request.isForMainFrame) return
        events.onPlayerPageFailed("error ${error.errorCode}")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (isReleased() || !request.isForMainFrame) return
        val code = errorResponse.statusCode
        if (code >= HTTP_SERVER_ERROR.first) {
            events.onPlayerPageFailed("HTTP $code")
        } else {
            AppLog.w(PLAYER_LOG_TAG, "Page answered HTTP $code")
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (isReleased()) return
        if (!isPlayerDocumentUrl(url)) {
            AppLog.w(PLAYER_LOG_TAG, "Left the player page, restoring it")
            events.restorePlayer()
            return
        }
        events.onPlayerPageLoaded()
        events.hideTopOverlay()
        OVERLAY_RETRY_DELAYS_MS.forEach { delay ->
            handler.postDelayed({ events.hideTopOverlay() }, delay)
        }
    }

    private companion object {
        val OVERLAY_RETRY_DELAYS_MS = longArrayOf(400, 1_200, 2_500)
    }
}
