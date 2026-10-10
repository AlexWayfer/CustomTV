package name.alexwayfer.customtv.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.FollowedLastBroadcastAge
import name.alexwayfer.customtv.data.followedLastBroadcastAge
import name.alexwayfer.customtv.data.followedLastBroadcastMillis
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

private val PREVIEW_WIDTH = 128.dp

@Composable
internal fun FollowedSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TwitchText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = modifier.semantics { heading() },
    )
}

/** The opaque band under a section title's text, where the list's loading bar runs. */
internal val SECTION_HEADER_BAND = 10.dp

/**
 * A section title that stays on top of its list while the rows scroll under it. It reports its height in pixels
 * through [onHeight], so the loading bar can run along its bottom.
 */
internal fun LazyListScope.sectionStickyHeader(key: Any, @StringRes text: Int, onHeight: (Int) -> Unit) {
    stickyHeader(key = key) {
        FollowedSectionHeader(
            text = stringResource(text),
            modifier = Modifier
                .animateItem()
                .onSizeChanged { onHeight(it.height) }
                .fillMaxWidth()
                .background(TwitchBg)
                .padding(bottom = SECTION_HEADER_BAND),
        )
    }
}

/** A live channel with its stream preview, the viewers over the preview, and the title and category beside it. */
@Composable
internal fun FollowedLiveChannelRow(
    channel: FollowedChannel,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = null,
                indication = ripple(color = Color.White),
                onClick = onWatch,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(PREVIEW_WIDTH)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(TwitchSurfaceAlt),
        ) {
            FollowedStreamPreview(url = followedPreviewUrl(channel), modifier = Modifier.fillMaxSize())
            if (channel.viewerCount != null) {
                ChannelListViewers(
                    viewerCount = channel.viewerCount,
                    sharedViewerCount = channel.sharedViewerCount,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelAvatarWithCollaborators(
                    channel = channel.login,
                    profile = followedChannelProfile(channel),
                    collaboratorAvatarUrls = channel.collaboratorAvatarUrls,
                    ringColor = TwitchBg,
                    size = 20.dp,
                )
                ChannelListTitle(
                    displayName = channel.displayName,
                    login = channel.login,
                    collaboratorCount = channel.collaborationCount,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .weight(1f, fill = false),
                )
            }
            if (!channel.streamTitle.isNullOrBlank()) {
                Text(
                    text = channel.streamTitle,
                    color = TwitchText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (channel.categoryName != null) {
                Text(
                    text = channel.categoryName,
                    color = TwitchTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FollowedWatchStreakLine(channelId = channel.id, isLive = true)
        }
    }
}

/** The preview keeps showing the previous image while a newer one loads, so a refresh never blanks it. */
@Composable
private fun FollowedStreamPreview(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var shownKey by remember { mutableStateOf<MemoryCache.Key?>(null) }
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .placeholderMemoryCacheKey(shownKey)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onSuccess = { state -> shownKey = state.result.memoryCacheKey },
    )
}

@Composable
internal fun FollowedOfflineChannelRow(
    channel: FollowedChannel,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = null,
                indication = ripple(color = Color.White),
                onClick = onWatch,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelAvatarWithCollaborators(
            channel = channel.login,
            profile = followedChannelProfile(channel),
            collaboratorAvatarUrls = channel.collaboratorAvatarUrls,
            ringColor = TwitchBg,
            size = 40.dp,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            ChannelListTitle(
                displayName = channel.displayName,
                login = channel.login,
                collaboratorCount = null,
            )
            FollowedWatchStreakLine(channelId = channel.id, isLive = false)
            val lastBroadcastAt = followedLastBroadcastMillis(channel)
            if (lastBroadcastAt != null) {
                val label = followedLastBroadcastLabel(lastBroadcastAt, System.currentTimeMillis())
                val description = stringResource(R.string.following_last_live, label)
                Text(
                    text = label,
                    color = TwitchTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                )
            }
        }
    }
}

private fun followedChannelProfile(channel: FollowedChannel) = ChannelProfile(
    login = channel.login,
    displayName = channel.displayName,
    avatarUrl = channel.avatarUrl,
    id = channel.id,
    categoryName = channel.categoryName,
    viewerCount = channel.viewerCount,
    isLive = channel.isLive,
)

@Composable
private fun followedLastBroadcastLabel(lastBroadcastAtMillis: Long, nowMillis: Long): String {
    return when (val age = followedLastBroadcastAge(lastBroadcastAtMillis, nowMillis)) {
        FollowedLastBroadcastAge.Today -> stringResource(R.string.following_last_live_today)
        FollowedLastBroadcastAge.Yesterday -> stringResource(R.string.following_last_live_yesterday)
        is FollowedLastBroadcastAge.DaysAgo ->
            pluralStringResource(R.plurals.following_last_live_days, age.days, age.days)
        is FollowedLastBroadcastAge.MonthsAgo ->
            pluralStringResource(R.plurals.following_last_live_months, age.months, age.months)
        is FollowedLastBroadcastAge.YearsAgo ->
            pluralStringResource(R.plurals.following_last_live_years, age.years, age.years)
    }
}
