package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

/** What a channel's Home shows besides its content. */
internal enum class ProfileHomeNotice { None, Loading, Failed, Empty }

/**
 * Home is empty only when everything it is made of loaded and holds nothing. Recordings that did not
 * load are a failure, even beside a live stream. Chapters that did not load have their own note under the
 * categories, and chapters still loading the first time keep the spinner when nothing else is shown.
 */
internal fun profileHomeNotice(
    recordings: ChannelRecordings,
    hasContent: Boolean,
    chaptersLoading: Boolean,
): ProfileHomeNotice {
    val page = (recordings as? ChannelRecordings.Ready)?.page
    return when {
        recordings == ChannelRecordings.Loading -> ProfileHomeNotice.Loading
        page == null -> ProfileHomeNotice.Failed
        hasContent || page.chaptersFailed.isNotEmpty() -> ProfileHomeNotice.None
        chaptersLoading -> ProfileHomeNotice.Loading
        else -> ProfileHomeNotice.Empty
    }
}

/** Home's notes under its content: chapters or recordings that did not load, the spinner, or that it is empty. */
@Composable
internal fun ColumnScope.ProfileHomeNotices(
    notice: ProfileHomeNotice,
    recordings: ChannelRecordingsState,
    chaptersFailed: Set<String>,
) {
    AnimatedVisibility(visible = chaptersFailed.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        ProfileLoadFailed(
            text = stringResource(R.string.profile_home_categories_incomplete),
            retrying = chaptersFailed.any { it in recordings.chaptersLoading },
            onRetry = { recordings.retryChapters(chaptersFailed) },
        )
    }
    ProfileHomeNoticeSlot(notice, onRetry = recordings::reload)
}

/**
 * The spinner, the failure, or the empty note, which cross-fade in one place: stacked, the next one would show
 * below the one fading out and jump up once it is gone.
 */
@Composable
internal fun ProfileHomeNoticeSlot(notice: ProfileHomeNotice, onRetry: () -> Unit) {
    Crossfade(targetState = notice, modifier = Modifier.fillMaxWidth(), label = "profile home notice") { shown ->
        when (shown) {
            ProfileHomeNotice.Failed -> ProfileLoadFailed(
                text = stringResource(R.string.profile_videos_failed),
                retrying = false,
                onRetry = onRetry,
            )
            ProfileHomeNotice.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(32.dp),
                )
            }
            ProfileHomeNotice.Empty -> ProfileHomeEmptyContent()
            ProfileHomeNotice.None -> Unit
        }
    }
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
