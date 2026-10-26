package name.alexwayfer.customtv.player

import android.webkit.WebView

/**
 * Whether the page fades the sound in and out, as the Smooth sound setting says. The page forgets it when it loads
 * again, so it is told again then, ahead of the script that starts the fade in of a video that has not played yet.
 */
internal class PlayerSmoothSound(
    private val webView: WebView,
    private val scripts: TwitchPlayerScripts,
    private val isReleased: () -> Boolean,
) {
    var enabled = true
        private set

    fun set(enabled: Boolean) {
        if (isReleased() || this.enabled == enabled) return
        this.enabled = enabled
        webView.evaluateJavascript(scripts.soundFade(playerSoundFadeMs(enabled)), null)
    }

    /** A page that loaded again has lost the setting. */
    fun pageLoaded() {
        if (!isReleased()) webView.evaluateJavascript(scripts.soundFade(playerSoundFadeMs(enabled)), null)
    }
}
