package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlinx.coroutines.flow.first

private const val LIVE_HEADER_KEY = "live-header"
private const val OFFLINE_HEADER_KEY = "offline-header"
private const val OFFLINE_GAP_KEY = "offline-gap"

// With the list's 4dp spacing on both sides, the offline title stays as far from the last live row as before.
private val OFFLINE_GAP = 12.dp

@Composable
internal fun FollowedChannelsSection(
    channels: List<FollowedChannel>,
    fromCache: Boolean,
    status: FollowedChannelsStatus,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (login: String, live: Boolean) -> Unit,
    scrollToTop: OneShotRequest<Unit>?,
    modifier: Modifier = Modifier,
) {
    FollowedWatchStreaksEffect(channels, fromCache)
    Column(modifier = modifier.fillMaxWidth()) {
        val loading = followedChannelsLoading(refreshing, status)
        var headerHeightPx by remember { mutableIntStateOf(0) }
        ListPullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            headerHeightPx = if (channels.isEmpty()) 0 else headerHeightPx,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (channels.isEmpty()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .height(maxHeight),
                    ) {
                        if (!loading) FollowedChannelsMessage(status)
                    }
                }
            } else {
                val listState = rememberLazyListState()
                var keepTop by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(channels) {
                    if (keepTop) listState.scrollToItem(0)
                }
                LaunchedEffect(scrollToTop) {
                    if (scrollToTop?.take() == null) return@LaunchedEffect
                    listState.animateScrollToItem(0)
                    keepTop = followedListKeepsTop(
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset,
                    )
                }
                LaunchedEffect(listState) {
                    listState.interactionSource.interactions.collect { interaction ->
                        if (interaction is DragInteraction.Start) keepTop = false
                        if (interaction is DragInteraction.Stop || interaction is DragInteraction.Cancel) {
                            snapshotFlow { listState.isScrollInProgress }.first { !it }
                            keepTop = followedListKeepsTop(
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset,
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val sections = followedSections(channels)
                    if (sections.live.isNotEmpty()) {
                        sectionStickyHeader(
                            key = LIVE_HEADER_KEY,
                            text = R.string.following_live_now,
                            onHeight = { headerHeightPx = it },
                        )
                        items(sections.live, key = { it.id }) { channel ->
                            FollowedLiveChannelRow(
                                channel = channel,
                                modifier = Modifier.animateItem(),
                                onWatch = { onOpen(channel.login, true) },
                            )
                        }
                    }
                    if (sections.offline.isNotEmpty()) {
                        // The gap between the sections is its own item, so the stuck header has no empty band above.
                        if (sections.live.isNotEmpty()) {
                            item(key = OFFLINE_GAP_KEY) {
                                Spacer(Modifier.animateItem().height(OFFLINE_GAP))
                            }
                        }
                        sectionStickyHeader(
                            key = OFFLINE_HEADER_KEY,
                            text = R.string.following_offline,
                            onHeight = { headerHeightPx = it },
                        )
                        items(sections.offline, key = { it.id }) { channel ->
                            FollowedOfflineChannelRow(
                                channel = channel,
                                modifier = Modifier.animateItem(),
                                onWatch = { onOpen(channel.login, false) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedChannelsMessage(status: FollowedChannelsStatus) {
    Text(
        text = stringResource(
            when (status) {
                FollowedChannelsStatus.Ready -> R.string.following_empty
                else -> R.string.following_unavailable
            },
        ),
        color = TwitchTextSecondary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** Whether the follow list is still on its way: a load runs, or the follow permission is being asked again. */
internal fun followedChannelsLoading(refreshing: Boolean, status: FollowedChannelsStatus): Boolean =
    refreshing || status == FollowedChannelsStatus.Loading || status == FollowedChannelsStatus.NeedsPermission
