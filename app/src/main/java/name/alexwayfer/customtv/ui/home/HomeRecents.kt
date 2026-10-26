package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.components.preview
import name.alexwayfer.customtv.ui.watch.OneShotRequest

private const val RECENTS_HEADER_KEY = "recents-header"
private const val LOG_IN_PROMPT_KEY = "log-in-prompt"

/**
 * The recent channels under their title, pulled down to refresh, with the log-in card under the title while it
 * shows. A channel newly on top scrolls up to it; coming back to Home with the same list keeps the place.
 */
@Composable
internal fun ColumnScope.HomeRecentChannels(
    viewModel: HomeViewModel,
    logInPrompt: HomeLogInPromptState,
    scrollToTop: OneShotRequest<Unit>?,
    onOpen: (channel: ChannelPreview, live: Boolean) -> Unit,
    onLogIn: () -> Unit,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val recentProfiles by viewModel.recentProfiles.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshingRecents.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    Spacer(Modifier.height(SECTION_GAP))
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    var shownFirstRecent by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(recents.firstOrNull()?.id) {
        val firstId = recents.firstOrNull()?.id
        if (firstId != null && firstId != shownFirstRecent) listState.scrollToItem(0)
        shownFirstRecent = firstId
    }
    LaunchedEffect(scrollToTop) {
        if (scrollToTop?.take() != null) listState.animateScrollToItem(0)
    }
    ListPullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refreshRecents() },
        headerHeightPx = headerHeightPx,
        modifier = Modifier.weight(1f),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            sectionStickyHeader(
                key = RECENTS_HEADER_KEY,
                text = R.string.recents,
                onHeight = { headerHeightPx = it },
            )
            // Under the title, so the loading bar keeps its place in the title's band.
            if (logInPrompt.shown) {
                item(key = LOG_IN_PROMPT_KEY) {
                    HomeLogInPromptCard(
                        onLogIn = onLogIn,
                        onDismiss = logInPrompt.dismiss,
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 4.dp),
                    )
                }
            }
            items(recents, key = { it.id }) { channel ->
                RecentChannelItem(
                    channel = channel.login,
                    refreshedProfile = recentProfiles[channel.login],
                    onWatch = { live ->
                        focusManager.clearFocus()
                        // Without a refreshed profile the row shows the cached one, which the profile finds too.
                        onOpen(recentProfiles[channel.login]?.preview() ?: ChannelPreview(channel.login), live)
                    },
                    onRemove = { viewModel.removeRecent(channel) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
