package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.JavascriptInterface

internal class PlayerPlaybackBridge(
    private val handler: Handler,
    private val onPlaybackState: (Boolean) -> Unit,
    private val onPositionSeconds: (Double) -> Unit,
    private val onSeekSeconds: (Double) -> Unit,
    private val onPlayerReady: () -> Unit,
    private val onOffline: () -> Unit,
    private val onPlayerError: () -> Unit,
    private val onPlayerReload: () -> Unit,
    private val onControlsVisible: (Boolean) -> Unit,
    private val onPageTouch: (scrollable: Boolean, onControls: Boolean, onVideo: Boolean) -> Unit,
    private val onControlBarFraction: (Double) -> Unit,
    private val onSettingsMenu: (Boolean) -> Unit,
    private val onHoverAreaMissing: () -> Unit,
) {
    @JavascriptInterface
    @Suppress("unused")
    fun onState(playing: Boolean) {
        handler.post { onPlaybackState(playing) }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPosition(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            handler.post { onPositionSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onSeek(seconds: Double) {
        if (seconds.isFinite() && seconds >= 0.0) {
            handler.post { onSeekSeconds(seconds) }
        }
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReady() {
        handler.post(onPlayerReady)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onOffline() {
        handler.post(onOffline)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerError() {
        handler.post(onPlayerError)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onPlayerReload() {
        handler.post(onPlayerReload)
    }

    @JavascriptInterface
    @Suppress("unused")
    fun onControlsVisible(visible: Boolean) {
        onControlsVisible.invoke(visible)
    }

    /**
     * Where a touch landed on the page: on scrollable content, on the player controls with the seek bar, or on the
     * bare video, where a click plays or pauses a recording.
     */
    @JavascriptInterface
    @Suppress("unused")
    fun onTouchStart(scrollable: Boolean, onControls: Boolean, onVideo: Boolean) {
        onPageTouch.invoke(scrollable, onControls, onVideo)
    }

    /** The page could not hide a recording's controls: the player area the pointer leaves is not where it was. */
    @JavascriptInterface
    @Suppress("unused")
    fun onHoverAreaMissing() {
        handler.post(onHoverAreaMissing)
    }

    /** Whether Twitch's settings menu is open over the player. */
    @JavascriptInterface
    @Suppress("unused")
    fun onSettingsMenu(open: Boolean) {
        handler.post { onSettingsMenu.invoke(open) }
    }

    /** The share of the page height under the top of the player's bottom-right control buttons. */
    @JavascriptInterface
    @Suppress("unused")
    fun onControlBar(fraction: Double) {
        if (fraction.isFinite() && fraction >= 0.0) {
            handler.post { onControlBarFraction(fraction) }
        }
    }
}
