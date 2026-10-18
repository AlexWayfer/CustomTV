package name.alexwayfer.customtv.player

import android.webkit.WebView

/**
 * The room the page opens between a recording's time labels and its seek bar, where the screen draws its own layer.
 * The page forgets it when it loads again, so it is opened again then.
 */
internal class PlayerSeekbarGap(
    private val webView: WebView,
    private val scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
) {
    private var gapDp = 0

    /** Opens room of [gapDp], or closes it with 0. */
    fun set(gapDp: Int) {
        if (isReleased() || this.gapDp == gapDp) return
        this.gapDp = gapDp
        webView.evaluateJavascript(scripts.seekbarGap(gapDp), null)
    }

    /** A page that loaded again has lost the room. */
    fun pageLoaded() {
        if (!isReleased() && gapDp > 0) webView.evaluateJavascript(scripts.seekbarGap(gapDp), null)
    }
}
