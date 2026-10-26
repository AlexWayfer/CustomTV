package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.player.StreamMediaArtwork
import name.alexwayfer.customtv.ui.components.CategoryGridSpacing
import name.alexwayfer.customtv.ui.components.CategoryTile
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.components.LiveStreamCard
import name.alexwayfer.customtv.ui.components.categoryGridColumns
import name.alexwayfer.customtv.ui.components.preview

/**
 * The categories of the recent recordings, most recent first, each once. Within a recording the
 * last chapter is the most recent; a recording without chapters counts its own category.
 */
internal fun recentCategories(videos: List<TwitchVideo>, chapters: Map<String, VodCategories>): List<TwitchCategory> {
    val found = linkedMapOf<String, TwitchCategory>()
    videos.forEach { video ->
        val categories = chapters[video.id] ?: return@forEach
        // A chapter without a game, such as "Chapter 1" of a stream started with none, is no category.
        val named = categories.chapters
            .filter { it.category.isNotBlank() && it.categoryId != null }
            .sortedByDescending { it.positionSeconds }
            .map { TwitchCategory(it.category, it.boxArtUrl, it.categoryId) }
            .ifEmpty {
                val fallbackId = categories.fallbackCategoryId ?: return@ifEmpty emptyList()
                listOfNotNull(
                    categories.fallbackCategory?.takeIf { it.isNotBlank() }
                        ?.let { TwitchCategory(it, categories.fallbackBoxArtUrl, fallbackId) },
                )
            }
        named.forEach { category ->
            val known = found[category.name]
            // A later chapter of the same category fills in what an earlier one lacked.
            found[category.name] = known?.copy(
                boxArtUrl = known.boxArtUrl ?: category.boxArtUrl,
                id = known.id ?: category.id,
            ) ?: category
        }
    }
    return found.values.toList()
}

@Composable
internal fun ChannelProfileHome(
    profile: ChannelProfile,
    recordings: ChannelRecordingsState,
    onOpenChannel: (ChannelPreview) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
    onOpenCategory: ((TwitchCategory) -> Unit)? = null,
) {
    val page = (recordings.recordings as? ChannelRecordings.Ready)?.page
    val videos = page?.videos.orEmpty()
    val chapters = page?.chapters.orEmpty()
    val categories = remember(videos, chapters) { recentCategories(videos, chapters) }
    val primary = profileHomePrimary(profile, profileHomePastBroadcast(videos), page?.suggestedChannels.orEmpty())
    val notice = profileHomeNotice(
        recordings = recordings.recordings,
        hasContent = primary != ProfileHomePrimary.None || categories.isNotEmpty(),
        chaptersLoading = recordings.chaptersLoading.isNotEmpty(),
    )
    // The page grows smoothly as the recordings load, and the spinner fades out as they come in.
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).animateContentSize()) {
        when (primary) {
            ProfileHomePrimary.Live -> {
                ProfileHomeHeading(stringResource(R.string.profile_home_live))
                LiveStreamCard(
                    profile = profile,
                    previewUrl = rememberProfilePreviewUrl(profile.login),
                    onClick = { onOpenChannel(profile.preview()) },
                    channelShown = false,
                )
            }
            is ProfileHomePrimary.Suggested -> {
                ProfileHomeHeading(stringResource(R.string.profile_home_suggested))
                LiveStreamCard(
                    profile = primary.channel,
                    previewUrl = rememberProfilePreviewUrl(primary.channel.login),
                    onClick = { onOpenChannel(primary.channel.preview()) },
                )
            }
            is ProfileHomePrimary.PastBroadcast -> ProfileHomePastBroadcast(
                video = primary.video,
                channelLogin = profile.login,
                chapters = recordings.chaptersOf(primary.video.id),
                onRetryChapters = { recordings.retryChapters(setOf(primary.video.id)) },
                onOpenVideo = onOpenVideo,
            )
            ProfileHomePrimary.None -> Unit
        }
        AnimatedVisibility(visible = categories.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column {
                ProfileHomeHeading(stringResource(R.string.profile_home_recent_categories))
                RecentCategoryGrid(categories, onOpenCategory, Modifier.padding(top = 8.dp))
            }
        }
        ProfileHomeNotices(notice, recordings, page?.chaptersFailed.orEmpty())
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

/** The recent categories as tiles; one with an ID opens through [onOpen], where the build can show a category. */
@Composable
private fun RecentCategoryGrid(
    categories: List<TwitchCategory>,
    onOpen: ((TwitchCategory) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = categoryGridColumns(maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(CategoryGridSpacing)) {
            categories.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CategoryGridSpacing)) {
                    row.forEach { category ->
                        CategoryTile(
                            category = category,
                            modifier = Modifier.weight(1f),
                            onClick = onOpen?.takeIf { category.id != null }?.let { open -> { open(category) } },
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** A channel's live preview, taken when the page first shows it. */
@Composable
private fun rememberProfilePreviewUrl(login: String): String =
    remember(login) { StreamMediaArtwork.previewUrl(login, System.currentTimeMillis()) }
