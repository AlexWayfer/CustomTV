package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.SharedViewerCount
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.components.rememberSecondTicker
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

@Composable
internal fun StreamLiveMeta(
    startedAtMillis: Long?,
    viewerCount: Int?,
    sharedViewerCount: Int?,
    onViewersClick: (() -> Unit)? = null,
) {
    val showSharedChannels = stringResource(R.string.collaboration_show)
    val now = rememberSecondTicker(running = startedAtMillis != null)
    val uptime = startedAtMillis?.let { formatStreamUptime(it, now) }
    val displayedViewers = viewerCount?.let { animateCount(it.toLong(), VIEWER_COUNT_ANIMATION_MS) }
    val viewers = displayedViewers?.let { NumberFormat.getIntegerInstance().format(it) }
    val liveDescription = uptime?.let { stringResource(R.string.stream_live_for, it) }
    val viewersDescription = viewerCount?.let { ownViewers ->
        val ownFormatted = NumberFormat.getIntegerInstance().format(ownViewers)
        if (sharedViewerCount != null) {
            stringResource(
                R.string.stream_viewers_with_shared,
                ownFormatted,
                NumberFormat.getIntegerInstance().format(sharedViewerCount),
            )
        } else {
            pluralStringResource(R.plurals.stream_viewers, ownViewers, ownFormatted)
        }
    }
    // A profile that loads after the screen opens fades its numbers in, rather than drawing them at once.
    val shownViewers = rememberLastNonNull(viewers?.let { it to viewersDescription.orEmpty() })
    val shownUptime = rememberLastNonNull(uptime?.let { it to liveDescription.orEmpty() })
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = viewers != null, enter = HeaderFadeIn, exit = HeaderFadeOut) {
            val (text, description) = shownViewers ?: return@AnimatedVisibility
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .semantics { contentDescription = description }
                    .then(
                        if (onViewersClick != null) {
                            Modifier.clickable(
                                    role = Role.Button,
                                    onClickLabel = showSharedChannels,
                                    onClick = onViewersClick,
                                )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                StreamViewersIcon(TwitchTextSecondary, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(text, color = TwitchText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                AnimatedVisibility(visible = sharedViewerCount != null, enter = HeaderFadeIn, exit = HeaderFadeOut) {
                    rememberLastNonNull(sharedViewerCount)?.let { SharedViewerCount(it, TwitchTextSecondary) }
                }
            }
        }
        AnimatedVisibility(visible = uptime != null, enter = HeaderFadeIn, exit = HeaderFadeOut) {
            val (text, description) = shownUptime ?: return@AnimatedVisibility
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(TwitchLive))
                Spacer(Modifier.width(6.dp))
                Text(text, color = TwitchText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun StreamViewersIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.12f
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val centerX = size.width / 2f
        val headRadius = size.minDimension * 0.22f
        val headCenter = Offset(centerX, size.height * 0.30f)
        drawCircle(tint, headRadius, headCenter, style = stroke)
        val bustTop = headCenter.y + headRadius + size.height * 0.10f
        val bust = Path().apply {
            moveTo(size.width * 0.08f, size.height - strokeWidth / 2f)
            quadraticTo(size.width * 0.14f, bustTop, centerX, bustTop)
            quadraticTo(size.width * 0.86f, bustTop, size.width * 0.92f, size.height - strokeWidth / 2f)
        }
        drawPath(bust, tint, style = stroke)
    }
}

private const val VIEWER_COUNT_ANIMATION_MS = 3000
