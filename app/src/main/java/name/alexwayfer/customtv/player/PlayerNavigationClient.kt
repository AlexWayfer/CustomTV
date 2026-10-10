package name.alexwayfer.customtv.player

import android.graphics.Bitmap
import android.os.Handler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import name.alexwayfer.customtv.diagnostics.AppLog

internal class PlayerNavigationClient(
    private val handler: Handler,
    private val isReleased: () -> Boolean,
    private val hideTopOverlay: () -> Unit,
    private val onPlayerPageStarted: () -> Unit,
    private val onPlayerPageLoaded: () -> Unit,
    private val onPlayerPageFailed: (reason: String) -> Unit,
    private val restorePlayer: () -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = TwitchPlayerScripts.shouldBlockNavigation(
        request.url,
        request.isForMainFrame,
    )

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        if (isReleased()) null else UsherAudioOnly.intercept(request)

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (isReleased() || !TwitchPlayerScripts.isPlayerDocumentUrl(url)) return
        onPlayerPageStarted()
        hideTopOverlay()
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        if (isReleased() || !TwitchPlayerScripts.isPlayerDocumentUrl(url)) return
        hideTopOverlay()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (isReleased() || !request.isForMainFrame) return
        onPlayerPageFailed("error ${error.errorCode}")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (isReleased() || !request.isForMainFrame) return
        val code = errorResponse.statusCode
        if (code >= 500) {
            onPlayerPageFailed("HTTP $code")
        } else {
            AppLog.w(PLAYER_LOG_TAG, "Page answered HTTP $code")
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (isReleased()) return
        if (!TwitchPlayerScripts.isPlayerDocumentUrl(url)) {
            AppLog.w(PLAYER_LOG_TAG, "Left the player page, restoring it")
            restorePlayer()
            return
        }
        onPlayerPageLoaded()
        hideTopOverlay()
        OVERLAY_RETRY_DELAYS_MS.forEach { delay ->
            handler.postDelayed({ hideTopOverlay() }, delay)
        }
    }

    private companion object {
        val OVERLAY_RETRY_DELAYS_MS = longArrayOf(400, 1_200, 2_500)
    }
}
