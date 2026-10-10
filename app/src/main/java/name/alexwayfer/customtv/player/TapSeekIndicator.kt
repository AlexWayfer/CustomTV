package name.alexwayfer.customtv.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.watch.FULLSCREEN_OVERLAY_CHAT_ALPHA
import kotlin.math.absoluteValue

/**
 * How far a series of seeking taps moved the recording, in the middle of the video: it pulses with every tap and fades
 * when the series ends.
 */
@Composable
internal fun BoxScope.TapSeekIndicator(series: TapSeekSeries?) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(series) {
        if (series == null) return@LaunchedEffect
        pulse.snapTo(TAP_SEEK_PULSE_SCALE)
        pulse.animateTo(1f)
    }
    val shown = rememberLastNonNull(series) ?: return
    AnimatedVisibility(
        visible = series != null,
        // In the middle, away from the tapping finger.
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Surface(
            modifier = Modifier
                .size(TapSeekIndicatorSize)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                },
            shape = CircleShape,
            color = Color.Black.copy(alpha = FULLSCREEN_OVERLAY_CHAT_ALPHA),
            contentColor = Color.White,
        ) {
            Text(
                text = pluralStringResource(
                    if (shown.seconds < 0) R.plurals.tap_seek_back else R.plurals.tap_seek_forward,
                    shown.seconds.absoluteValue,
                    shown.seconds.absoluteValue,
                ),
                modifier = Modifier.wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val TapSeekIndicatorSize = 64.dp
private const val TAP_SEEK_PULSE_SCALE = 1.12f
