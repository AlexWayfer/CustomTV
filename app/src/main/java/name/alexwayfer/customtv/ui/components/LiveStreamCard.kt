package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.home.ChannelAvatarWithCollaborators
import name.alexwayfer.customtv.ui.home.ChannelListTitle
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.watch.StreamViewersIcon
import name.alexwayfer.customtv.ui.watch.formatStreamUptime
import java.text.NumberFormat

private val LiveStreamAvatarSize = 40.dp

/**
 * A live stream as a card: its preview across the width, with the viewers in one bottom corner and how long it has
 * been live in the other, and the title and category under it. With [channelShown] the channel's avatar, with its
 * collaborators', and its name join them, for a stream of another channel than the page's own. [tags] go under the
 * text.
 */
@Composable
internal fun LiveStreamCard(
    profile: ChannelProfile,
    previewUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    channelShown: Boolean = true,
    tags: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LiveStreamPreview(profile, previewUrl)
        Row {
            if (channelShown) {
                ChannelAvatarWithCollaborators(
                    channel = profile.login,
                    profile = profile,
                    collaboratorAvatarUrls = profile.collaboratorAvatarUrls,
                    ringColor = TwitchBg,
                    size = LiveStreamAvatarSize,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = profile.streamTitle?.takeIf { it.isNotBlank() } ?: profile.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channelShown) {
                    ChannelListTitle(
                        displayName = profile.displayName,
                        login = profile.login,
                        collaboratorCount = profile.collaborationCount,
                    )
                }
                profile.categoryName?.let { category ->
                    Text(
                        text = category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                tags?.invoke()
            }
        }
    }
}

/** The stream's preview with the viewers in one bottom corner and how long it has been live in the other. */
@Composable
private fun LiveStreamPreview(profile: ChannelProfile, previewUrl: String) {
    val startedAtMillis = profile.streamStartedAtMillis
    val nowMillis = rememberSecondTicker(running = startedAtMillis != null)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT_RATIO)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        StreamPreviewImage(url = previewUrl, modifier = Modifier.fillMaxSize())
        profile.viewerCount?.let { viewers ->
            LiveStreamViewers(viewers, profile.sharedViewerCount, Modifier.align(Alignment.BottomStart))
        }
        startedAtMillis?.let { started ->
            val uptime = formatStreamUptime(started, nowMillis)
            val description = stringResource(R.string.stream_live_for, uptime)
            LiveStreamBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(TwitchLive))
                Spacer(Modifier.width(4.dp))
                Text(uptime, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** The stream's own viewers, and the audience of its whole collaboration after them when it has one. */
@Composable
private fun LiveStreamViewers(viewers: Int, sharedViewerCount: Int?, modifier: Modifier = Modifier) {
    val formatted = NumberFormat.getIntegerInstance().format(viewers)
    val description = if (sharedViewerCount != null) {
        stringResource(
            R.string.stream_viewers_with_shared,
            formatted,
            NumberFormat.getIntegerInstance().format(sharedViewerCount),
        )
    } else {
        pluralStringResource(R.plurals.stream_viewers, viewers, formatted)
    }
    LiveStreamBadge(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        StreamViewersIcon(Color.White, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(formatted, color = Color.White, style = MaterialTheme.typography.labelMedium)
        if (sharedViewerCount != null) SharedViewerCount(sharedViewerCount, Color.White)
    }
}

@Composable
private fun LiveStreamBadge(modifier: Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
