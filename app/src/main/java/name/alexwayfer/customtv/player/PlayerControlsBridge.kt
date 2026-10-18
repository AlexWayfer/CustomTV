package name.alexwayfer.customtv.player

import android.os.Handler
import android.webkit.JavascriptInterface

/** What the page tells about the player's own controls and the touches on them, as `CustomTvControls`. */
internal class PlayerControlsBridge(
    private val handler: Handler,
    private val onControlsVisible: (Boolean) -> Unit,
    private val onPageTouch: (
        scrollable: Boolean,
        onControls: Boolean,
        onVideo: Boolean,
        onDisclosure: Boolean,
    ) -> Unit,
    private val onControlBarFraction: (Double) -> Unit,
    private val onSeekbar: (PlayerSeekbar) -> Unit,
    private val onSettingsMenu: (Boolean) -> Unit,
    private val onHoverAreaMissing: () -> Unit,
) {
    @JavascriptInterface
    @Suppress("unused")
    fun onControlsVisible(visible: Boolean) {
        onControlsVisible.invoke(visible)
    }

    /**
     * Where a touch landed on the page: on scrollable content, on the player controls with the seek bar, or on the
     * bare video, where a click plays or pauses a recording, or on a disclosure, such as the audience plaque.
     */
    @JavascriptInterface
    @Suppress("unused")
    fun onTouchStart(scrollable: Boolean, onControls: Boolean, onVideo: Boolean, onDisclosure: Boolean) {
        onPageTouch.invoke(scrollable, onControls, onVideo, onDisclosure)
    }

    /** Where a recording's seek bar sits, as shares of the page; see [PlayerSeekbar]. */
    @JavascriptInterface
    @Suppress("unused")
    fun onSeekbar(left: Double, right: Double, barTop: Double, touchTop: Double) {
        val seekbar = playerSeekbarOrNull(left, right, barTop, touchTop) ?: return
        handler.post { onSeekbar.invoke(seekbar) }
    }

    /** The page could not hide a recording's controls: the player area the pointer leaves is not where it was. */
    @JavascriptInterface
    @Suppress("unused")
    fun onHoverAreaMissing() {
        handler.post(onHoverAreaMissing)
    }

    /** Whether Twitch's settings menu, or a recording's chapter list, is open over the player. */
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
