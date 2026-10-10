package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The stream info over the top of a full screen player, with its controls. A dark gradient keeps the text readable
 * on any frame and fades into the video below it; the sleep timer and the settings gear in the top corner keep their
 * room.
 */
@Composable
internal fun BoxScope.PlayerFullscreenInfo(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FullscreenInfoScrim)
                .padding(end = 88.dp, bottom = 24.dp),
        ) {
            content()
        }
    }
}

/**
 * How far the buttons in the full screen player's top corner sit below its top: their middles line up with the
 * stream info's heart and bell (the panel's 10.dp top padding plus half of a 40.dp button, less half of a 44.dp one).
 */
internal val FullscreenTopButtonsTop = 8.dp

// Dark behind the whole text, down to its grey last line, and fading out only in the room under it.
private val FullscreenInfoScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.8f),
    0.7f to Color.Black.copy(alpha = 0.65f),
    1f to Color.Transparent,
)
