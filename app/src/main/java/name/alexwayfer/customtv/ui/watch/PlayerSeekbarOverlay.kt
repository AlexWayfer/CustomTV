package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import name.alexwayfer.customtv.player.PlayerSeekbar

/** What a layer over a recording's seek bar reads: where the bar sits on the page, and whether the layer shows. */
internal data class PlayerSeekbarSpot(val seekbar: PlayerSeekbar, val shown: Boolean)

/**
 * A layer the screen draws over a recording's seek bar, in the room of [gap] that the page opens between the bar and
 * its time labels. The layer fills the video and places itself by the [PlayerSeekbarSpot] it gets.
 */
internal class PlayerSeekbarOverlay(
    val gap: Dp,
    val content: @Composable BoxScope.(PlayerSeekbarSpot) -> Unit,
)

/**
 * The layer over the seek bar shows with Twitch's controls on a full player that stays in place, once the video has
 * loaded. It hides as the player starts to move, and while Twitch's settings menu or chapter list is open: drawn over
 * the page, it would cover them.
 */
internal fun playerSeekbarOverlayShown(
    controlsVisible: Boolean,
    videoLoading: Boolean,
    settingsMenuOpen: Boolean,
    progress: Float,
): Boolean = controlsVisible && !videoLoading && !settingsMenuOpen && progress == 0f
