package name.alexwayfer.customtv.player

import android.content.res.AssetManager
import java.util.concurrent.ConcurrentHashMap

/**
 * The scripts the app runs in the Twitch player page. Each one is a file in `assets/player`, read once a process on
 * first use, and runs after `common.js`, which holds the helpers they share.
 */
internal class TwitchPlayerScripts(private val assets: AssetManager) {
    val play: String get() = script("play.js")

    val pause: String get() = script("pause.js")

    /**
     * Shows a recording's controls with a pointer that enters the player and moves over the video: a tap there
     * would play or pause it. They hide on Twitch's own timer, or at once with [hideControls].
     */
    val showControls: String get() = script("show-controls.js")

    /**
     * Hides a recording's shown controls at once with a pointer that leaves the player; a tap on the video would
     * also play or pause it. When the page shows controls without that player area, the app hears once per page
     * load, so a Twitch change shows in the report.
     */
    val hideControls: String get() = script("hide-controls.js")

    val reloadPlayer: String get() = script("reload-player.js")

    val hideTopOverlay: String get() = script("hide-top-overlay.js")

    /**
     * Switches the player to sound only, remembering whether it chose the quality itself or played a fixed one.
     * Does nothing while the playlist lacks the audio-only rendition.
     */
    val soundOnly: String get() = script("sound-only.js")

    /** Sets the player's own volume to full, so only the system volume and the mute button change the loudness. */
    val fullVolume: String get() = script("full-volume.js")

    /** Brings the video back as it played before [soundOnly]: the player's own choice or the fixed quality. */
    val videoQuality: String get() = script("video-quality.js")

    /**
     * Plays the quality of [mode], `best` or `auto`, saving the viewer's own first, or brings that one back with
     * `user`, handing it to sound only when that plays now.
     */
    fun quality(mode: String): String = call("quality.js", "'$mode'")

    /** Hides the audience plaque over a [zoomed] picture, which the page zooms along, or shows it again. */
    fun zoomOverlays(zoomed: Boolean): String = call("zoom-overlays.js", zoomed.toString())

    /**
     * Opens room of [gapDp] between a recording's time labels and its seek bar, where the app draws over the page, or
     * closes it with 0. The page's CSS pixel is a dp.
     */
    fun seekbarGap(gapDp: Int): String = call("seekbar-gap.js", gapDp.toString())

    /** Moves a recording to [seconds] through Twitch's player, or through the video when the player is not found. */
    fun seek(seconds: Double): String = call("seek.js", seconds.toString())

    /**
     * Hides the player's control bar in picture-in-picture, where the system draws its own controls over the small
     * window, or shows it again. The bar keeps its layout, so the reported control bar height stays.
     */
    fun pictureInPicture(enabled: Boolean): String = call("picture-in-picture.js", enabled.toString())

    private fun script(name: String): String = "${load(COMMON)}\n${load(name)}"

    /** A script that is a function expression, called with [argument]. */
    private fun call(name: String, argument: String): String = "${load(COMMON)}\n${load(name).trimEnd()}($argument);"

    private fun load(name: String): String = loaded.getOrPut(name) {
        assets.open("$DIRECTORY/$name").bufferedReader().use { it.readText() }
    }

    private companion object {
        const val DIRECTORY = "player"
        const val COMMON = "common.js"
        val loaded = ConcurrentHashMap<String, String>()
    }
}
