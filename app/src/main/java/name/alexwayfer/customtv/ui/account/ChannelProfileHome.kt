package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.NumberFormat
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.player.StreamMediaArtwork
import name.alexwayfer.customtv.ui.components.rememberSecondTicker
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.watch.StreamViewersIcon
import name.alexwayfer.customtv.ui.watch.formatStreamUptime

private val CategoryTileMinWidth = 120.dp
private val CategoryGridSpacing = 12.dp
private const val CATEGORY_GRID_MIN_COLUMNS = 3

internal sealed interface ProfileHomePrimary {
    data object Live : ProfileHomePrimary
    data class PastBroadcast(val video: TwitchVideo) : ProfileHomePrimary
    data class Suggested(val channel: ChannelProfile) : ProfileHomePrimary
    data object None : ProfileHomePrimary
}

/**
 * A live stream comes first; otherwise the recording the build offers; otherwise the first
 * suggested channel that is live. Offline suggestions show nothing.
 */
internal fun profileHomePrimary(
    profile: ChannelProfile,
    pastBroadcast: TwitchVideo?,
    suggestedChannels: List<ChannelProfile>,
): ProfileHomePrimary {
    val suggested = suggestedChannels.firstOrNull { it.isLive }
    return when {
        profile.isLive -> ProfileHomePrimary.Live
        pastBroadcast != null -> ProfileHomePrimary.PastBroadcast(pastBroadcast)
        suggested != null -> ProfileHomePrimary.Suggested(suggested)
        else -> ProfileHomePrimary.None
    }
}

internal data class RecentCategory(val name: String, val boxArtUrl: String?)

/**
 * The categories of the recent recordings, most recent first, each once. Within a recording the
 * last chapter is the most recent; a recording without chapters counts its own category.
 */
internal fun recentCategories(videos: List<TwitchVideo>, chapters: Map<String, VodCategories>): List<RecentCategory> {
    val found = linkedMapOf<String, RecentCategory>()
    videos.forEach { video ->
        val categories = chapters[video.id] ?: return@forEach
        val named = categories.chapters
            .filter { it.category.isNotBlank() }
            .sortedByDescending { it.positionSeconds }
            .map { RecentCategory(it.category, it.boxArtUrl) }
            .ifEmpty {
                listOfNotNull(
                    categories.fallbackCategory?.takeIf { it.isNotBlank() }
                        ?.let { RecentCategory(it, categories.fallbackBoxArtUrl) },
                )
            }
        named.forEach { category ->
            val known = found[category.name]
            if (known?.boxArtUrl == null) found[category.name] = category
        }
    }
    return found.values.toList()
}

/** Three tiles on a phone; a wider screen adds a column for each [CategoryTileMinWidth]. */
internal fun categoryGridColumns(width: Dp): Int =
    ((width + CategoryGridSpacing) / (CategoryTileMinWidth + CategoryGridSpacing)).toInt()
        .coerceAtLeast(CATEGORY_GRID_MIN_COLUMNS)

@Composable
internal fun ChannelProfileHome(
    profile: ChannelProfile,
    recordings: ChannelRecordings,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
) {
    val page = (recordings as? ChannelRecordings.Ready)?.page
    val videos = page?.videos.orEmpty()
    val chapters = page?.chapters.orEmpty()
    val categories = remember(videos, chapters) { recentCategories(videos, chapters) }
    val primary = profileHomePrimary(profile, profileHomePastBroadcast(videos), page?.suggestedChannels.orEmpty())
    // The page grows smoothly as the recordings load, and the spinner fades out as they come in.
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).animateContentSize()) {
        when (primary) {
            ProfileHomePrimary.Live -> {
                ProfileHomeHeading(stringResource(R.string.profile_home_live))
                ProfileLiveItem(profile, showChannelName = false, onClick = { onOpenChannel(profile.login) })
            }
            is ProfileHomePrimary.Suggested -> {
                ProfileHomeHeading(stringResource(R.string.profile_home_suggested))
                ProfileLiveItem(
                    profile = primary.channel,
                    showChannelName = true,
                    onClick = { onOpenChannel(primary.channel.login) },
                )
            }
            is ProfileHomePrimary.PastBroadcast -> ProfileHomePastBroadcast(
                video = primary.video,
                channelLogin = profile.login,
                chapters = chapters[primary.video.id],
                onOpenVideo = onOpenVideo,
            )
            ProfileHomePrimary.None -> Unit
        }
        AnimatedVisibility(visible = categories.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column {
                ProfileHomeHeading(stringResource(R.string.profile_home_recent_categories))
                RecentCategoryGrid(categories, Modifier.padding(top = 8.dp))
            }
        }
        AnimatedVisibility(
            visible = recordings == ChannelRecordings.Loading,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(32.dp),
            )
        }
        AnimatedVisibility(
            visible = recordings != ChannelRecordings.Loading && primary == ProfileHomePrimary.None && categories.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ProfileHomeEmptyContent()
        }
    }
}

@Composable
internal fun ProfileHomeHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ProfileHomeEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.profile_home_empty_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.profile_home_empty_body),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RecentCategoryGrid(categories: List<RecentCategory>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = categoryGridColumns(maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(CategoryGridSpacing)) {
            categories.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CategoryGridSpacing)) {
                    row.forEach { category ->
                        RecentCategoryItem(category, Modifier.weight(1f))
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCategoryItem(category: RecentCategory, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            category.boxArtUrl?.let { boxArt ->
                AsyncImage(
                    model = boxArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = category.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ProfileLiveItem(profile: ChannelProfile, showChannelName: Boolean, onClick: () -> Unit) {
    val preview = remember(profile.login) { StreamMediaArtwork.previewUrl(profile.login, System.currentTimeMillis()) }
    val startedAtMillis = profile.streamStartedAtMillis
    val nowMillis = rememberSecondTicker(running = startedAtMillis != null)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = preview,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            profile.viewerCount?.let { viewers ->
                val formatted = NumberFormat.getIntegerInstance().format(viewers)
                val description = pluralStringResource(R.plurals.stream_viewers, viewers, formatted)
                LiveBadge(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clearAndSetSemantics { contentDescription = description },
                ) {
                    StreamViewersIcon(Color.White, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatted, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            startedAtMillis?.let { started ->
                val uptime = formatStreamUptime(started, nowMillis)
                val description = stringResource(R.string.stream_live_for, uptime)
                LiveBadge(
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
        Column {
            Text(
                text = profile.streamTitle?.takeIf { it.isNotBlank() } ?: profile.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showChannelName) {
                Text(
                    text = profile.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier, content: @Composable () -> Unit) {
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
