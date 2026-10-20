package name.alexwayfer.customtv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.watch.FULLSCREEN_OVERLAY_CHAT_ALPHA
import name.alexwayfer.customtv.ui.watch.HeaderFadeIn
import name.alexwayfer.customtv.ui.watch.HeaderFadeOut

/** Play or Pause in the middle of the player, fading with the controls; [button] is null while neither shows. */
@Composable
internal fun BoxScope.PlayerPlayPauseButton(button: PlayerCenterButton?) {
    val shown = rememberLastNonNull(button) ?: return
    AnimatedVisibility(
        visible = button != null,
        modifier = Modifier.align(Alignment.Center),
        enter = HeaderFadeIn,
        exit = HeaderFadeOut,
    ) {
        val label = stringResource(
            if (shown == PlayerCenterButton.Play) R.string.player_play else R.string.player_pause,
        )
        Surface(
            onClick = {
                when (shown) {
                    PlayerCenterButton.Play -> StreamPlayback.play()
                    PlayerCenterButton.Pause -> StreamPlayback.pause()
                }
            },
            modifier = Modifier
                .size(TapSeekIndicatorSize)
                .semantics { contentDescription = label },
            shape = CircleShape,
            color = Color.Black.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA),
            contentColor = Color.White,
        ) {
            Crossfade(targetState = shown, label = "play pause icon") { icon ->
                // Crossfade does not pass the circle's size down: without it the icon sits in the top left corner.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (icon) {
                        PlayerCenterButton.Play -> Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(PlayPauseIconSize),
                        )
                        PlayerCenterButton.Pause -> Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = null,
                            modifier = Modifier.size(PlayPauseIconSize),
                        )
                    }
                }
            }
        }
    }
}

private val PlayPauseIconSize = 36.dp
