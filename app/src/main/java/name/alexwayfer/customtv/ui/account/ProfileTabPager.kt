package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/** What the profile does once a swipe comes to rest on a page. */
internal sealed interface ProfilePageSettled {
    data class ShowTab(val tab: Int) : ProfilePageSettled

    /** The page past the last tab stands for Chat: it opens the stream, and the profile goes back to its first tab. */
    data object OpenChat : ProfilePageSettled
}

internal fun profilePageSettled(page: Int, tabCount: Int): ProfilePageSettled =
    if (page >= tabCount) ProfilePageSettled.OpenChat else ProfilePageSettled.ShowTab(page)

/**
 * The least height of the tabs, so their pages reach the bottom of [viewportPx] and a swipe below a short page still
 * moves between tabs. The rest of the profile, [contentPx] without [tabsPx], keeps its height.
 */
internal fun profileTabsMinHeightPx(viewportPx: Int, contentPx: Int, tabsPx: Int): Int =
    (viewportPx - (contentPx - tabsPx)).coerceAtLeast(0)

/**
 * The profile's tabs over pages that follow a swipe, with Chat last. [tabTitles] name the tabs before Chat, and
 * [page] draws the tab at an index.
 */
@Composable
internal fun ProfileTabPager(
    tabTitles: List<Int>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenChat: () -> Unit,
    minHeight: Dp,
    tabRowPin: () -> Int,
    modifier: Modifier = Modifier,
    page: @Composable ColumnScope.(tab: Int) -> Unit,
) {
    val density = LocalDensity.current
    var tabRowHeightPx by remember { mutableIntStateOf(0) }
    val pagerMinHeight = (minHeight - with(density) { tabRowHeightPx.toDp() }).coerceAtLeast(0.dp)
    val tabCount = tabTitles.size
    val pagerState = rememberPagerState(initialPage = selectedTab) { tabCount + 1 }
    val scope = rememberCoroutineScope()
    val currentOnSelectTab by rememberUpdatedState(onSelectTab)
    val currentOnOpenChat by rememberUpdatedState(onOpenChat)
    // A tab chosen elsewhere, such as Back or a new profile, moves the pages too.
    LaunchedEffect(selectedTab) {
        if (pagerState.settledPage != selectedTab) pagerState.animateScrollToPage(selectedTab)
    }
    LaunchedEffect(pagerState.settledPage) {
        when (val settled = profilePageSettled(pagerState.settledPage, tabCount)) {
            is ProfilePageSettled.ShowTab -> currentOnSelectTab(settled.tab)
            ProfilePageSettled.OpenChat -> {
                pagerState.scrollToPage(PROFILE_TAB_FIRST)
                currentOnSelectTab(PROFILE_TAB_FIRST)
                currentOnOpenChat()
            }
        }
    }
    Column(modifier) {
        // Each tab is as wide as its title, and the row scrolls when they do not fit, as with a large font. It reaches
        // the screen's edges, and its first title lines up with the profile's text.
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            // Pinned under the profile's bar, the row covers the pages that scroll on under it.
            modifier = Modifier
                .bleedPastProfileSides()
                .onSizeChanged { tabRowHeightPx = it.height }
                .zIndex(1f)
                .graphicsLayer { translationY = tabRowPin().toFloat() },
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = ProfileSideMargin - TabTitleSidePadding,
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(title)) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A tap goes the way a swipe does: the pages move to Chat, and resting there opens the stream.
            ChannelChatTab(
                selected = pagerState.currentPage == tabCount,
                onClick = { scope.launch { pagerState.animateScrollToPage(tabCount) } },
            )
        }
        HorizontalPager(
            state = pagerState,
            // The pages differ in height; the profile grows or shrinks to the one that comes to rest.
            modifier = Modifier.animateContentSize().heightIn(min = pagerMinHeight),
            verticalAlignment = Alignment.Top,
        ) { index ->
            Column {
                if (index < tabCount) page(index)
            }
        }
    }
}

/** The room Material 3 keeps on each side of a tab's title. */
private val TabTitleSidePadding = 16.dp
