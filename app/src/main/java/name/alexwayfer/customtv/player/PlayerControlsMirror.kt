package name.alexwayfer.customtv.player

import android.os.Handler
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView

/** Whether Twitch's player shows its controls, and bringing them in line with the app's own chrome. */
internal class PlayerControlsMirror(
    private val handler: Handler,
    private val webView: WebView,
    private val scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
    private val isRecording: () -> Boolean,
    private val onVisible: (Boolean) -> Unit,
) {
    @Volatile
    var visible = false
        private set

    /** The app is tapping the player itself, so the gestures let the tap through. */
    var dispatchingTap = false
        private set

    fun setVisible(visible: Boolean) {
        this.visible = visible
        handler.post { onVisible(visible) }
    }

    /** Acts after the current pass, once the page has its final size, if the controls still differ. */
    fun match(visible: Boolean) {
        handler.post {
            if (isReleased()) return@post
            when (playerControlsSync(isRecording(), StreamPlayback.userPaused, visible, this.visible)) {
                PlayerControlsSync.None -> Unit
                PlayerControlsSync.Tap -> dispatchPlayerTap()
                PlayerControlsSync.ShowWithoutTap ->
                    webView.evaluateJavascript(scripts.showControls, null)
                PlayerControlsSync.HideWithoutTap ->
                    webView.evaluateJavascript(scripts.hideControls, null)
            }
        }
    }

    /** Taps the player's center past the gestures; the chrome already shows the result. */
    private fun dispatchPlayerTap() {
        val x = webView.width / 2f
        val y = webView.height / 2f
        if (x <= 0f || y <= 0f) return
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0)
        dispatchingTap = true
        try {
            webView.dispatchTouchEvent(down)
            webView.dispatchTouchEvent(up)
        } finally {
            dispatchingTap = false
        }
        down.recycle()
        up.recycle()
    }
}
