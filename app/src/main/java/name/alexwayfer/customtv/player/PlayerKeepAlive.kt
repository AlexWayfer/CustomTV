package name.alexwayfer.customtv.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.webkit.WebView
import androidx.core.content.ContextCompat

// How often the page playing in the background is woken and told to play.
private const val KEEP_ALIVE_MS = 2_000L

/** Keeps the player page playing while the app is in the background, in picture-in-picture, or the screen is off. */
internal class PlayerKeepAlive(
    private val appContext: Context,
    private val handler: Handler,
    private val webView: WebView,
    scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
) {
    private var inBackground = false
    private val keepAlive = object : Runnable {
        override fun run() {
            if (isReleased() || !inBackground) return
            webView.onResume()
            webView.resumeTimers()
            if (!StreamPlayback.userPaused) {
                webView.evaluateJavascript(scripts.play, null)
            }
            handler.postDelayed(this, KEEP_ALIVE_MS)
        }
    }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                keepAwake(true)
            }
        }
    }

    fun register() {
        ContextCompat.registerReceiver(
            appContext,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun keepAwake(background: Boolean) {
        if (isReleased()) return
        if (background && inBackground) return
        webView.onResume()
        webView.resumeTimers()
        inBackground = background
        StreamPlayback.inferredPauseSuppressed = background
        handler.removeCallbacks(keepAlive)
        if (background) {
            handler.postDelayed(keepAlive, KEEP_ALIVE_MS)
        }
    }

    fun release() {
        inBackground = false
        StreamPlayback.inferredPauseSuppressed = false
        try {
            appContext.unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
            // The receiver was never registered.
        }
    }
}
