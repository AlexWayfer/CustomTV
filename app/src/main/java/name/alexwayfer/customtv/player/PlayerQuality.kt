package name.alexwayfer.customtv.player

import android.webkit.WebView

/** The video quality the app asks of the player; [script] names it to `quality.js`. */
internal enum class PlayerQualityMode(val script: String) {
    /** The viewer's own quality, as chosen in the player's menu. */
    User("user"),

    /** The player chooses by the connection and its size. */
    Auto("auto"),

    /** The highest quality the stream or recording has. */
    Best("best"),
}

/** A zoomed picture plays in the best quality, full screen lets the player choose, and otherwise the viewer's own. */
internal fun playerQualityMode(zoomed: Boolean, fullscreen: Boolean): PlayerQualityMode = when {
    zoomed -> PlayerQualityMode.Best
    fullscreen -> PlayerQualityMode.Auto
    else -> PlayerQualityMode.User
}

/** Sets the player's quality as the zoom and full screen change, and brings the viewer's own back after them. */
internal class PlayerQuality(
    private val webView: WebView,
    private val scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
) {
    private var zoomed = false
    private var fullscreen = false

    /** The mode the page plays in; null on a new page until it plays, which then gets the current mode once. */
    private var applied: PlayerQualityMode? = null

    /** The zoom rests, [zoomed] or whole. */
    fun settleZoom(zoomed: Boolean) {
        this.zoomed = zoomed
        apply()
    }

    fun setFullscreen(fullscreen: Boolean) {
        this.fullscreen = fullscreen
        apply()
    }

    /** A new page loads: it starts in whatever quality Twitch saved, so it gets the current mode once it plays. */
    fun pageLoading() {
        applied = null
    }

    /** The page plays; a page that has not had its mode yet gets it now. */
    fun playing() {
        if (applied == null) apply(force = true)
    }

    private fun apply(force: Boolean = false) {
        val mode = playerQualityMode(zoomed, fullscreen)
        if (!force && (applied == null || mode == applied)) return
        applied = mode
        if (!isReleased()) webView.evaluateJavascript(scripts.quality(mode.script), null)
    }
}
