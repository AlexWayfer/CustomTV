package name.alexwayfer.customtv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/**
 * How far the picture is zoomed, at the top of the player while it is: a tap brings back the whole picture. [scale] is
 * null while the picture is whole.
 */
@Composable
internal fun BoxScope.PlayerZoomResetButton(scale: Float?, onReset: () -> Unit) {
    val shown = rememberLastNonNull(scale) ?: return
    val resetLabel = stringResource(R.string.player_zoom_reset)
    AnimatedVisibility(
        visible = scale != null,
        // The chrome hides while zoomed, so the top middle is free.
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            onClick = onReset,
            modifier = Modifier.semantics { contentDescription = resetLabel },
            shape = CircleShape,
            color = Color.Black.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA),
            contentColor = Color.White,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_zoom_out),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.player_zoom_scale, shown),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
