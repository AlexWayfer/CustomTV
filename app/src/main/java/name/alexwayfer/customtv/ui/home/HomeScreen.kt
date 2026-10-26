package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.account.TwitchUserAccess
import name.alexwayfer.customtv.ui.components.AppTitle
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.notifications.HomeAlertsPrompts
import name.alexwayfer.customtv.ui.notifications.HomeNotificationPrompt
import name.alexwayfer.customtv.ui.rememberLifecycleResumed
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** From the title row to the title of the list below it. */
internal val SECTION_GAP = 16.dp

/** Material's icon button, the search button beside the title. */
private val SEARCH_BUTTON_SIZE = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    homeVisible: Boolean,
    signedIn: Boolean,
    followsReady: Boolean,
    signedInUserId: String?,
    followAccess: suspend () -> TwitchUserAccess?,
    earlyFollowAccess: () -> TwitchUserAccess?,
    onFollowPermissionRejected: () -> Unit,
    scrollListToTop: OneShotRequest<Unit>?,
    onWatch: (ChannelPreview) -> Unit,
    onOpenProfile: (ChannelPreview) -> Unit,
    onLogIn: () -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    padForIme: Boolean = true,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val recentsRestored by viewModel.recentsRestored.collectAsStateWithLifecycle()
    val followedChannels by viewModel.follows.channels.collectAsStateWithLifecycle()
    val followedStatus by viewModel.follows.status.collectAsStateWithLifecycle()
    val searchState = rememberSearchBarState()
    val searchEntry = homeSearchEntry(signedIn, recentsRestored, recents.isNotEmpty())
    val logInPrompt = rememberHomeLogInPrompt(signedIn)
    val channelInput = rememberSaveable { mutableStateOf("") }
    val appInForeground = rememberLifecycleResumed()
    RecentsAutoRefresh(viewModel, homeVisible, appInForeground, recents, signedIn)
    // The search opens in its own window, which would stay over a player or another screen covering Home.
    LaunchedEffect(homeVisible) {
        if (!homeVisible) searchState.snapTo(0f)
    }
    LaunchedEffect(signedIn, followedStatus) {
        if (signedIn && followedStatus == FollowedChannelsStatus.NeedsPermission) onFollowPermissionRejected()
    }
    FollowsAutoRefresh(
        follows = viewModel.follows,
        active = signedIn && homeVisible && appInForeground,
        userId = signedInUserId,
        access = followAccess,
        earlyAccess = earlyFollowAccess,
    )
    val open = homeChannelOpener(onWatch, onOpenProfile)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TwitchBg)
                .statusBarsPadding()
                .then(if (padForIme) Modifier.imePadding() else Modifier)
                .padding(horizontal = 16.dp),
        ) {
            HomeTitleRow(searchState, searchShown = searchEntry == HomeSearchEntry.Button)
            HomeNotificationPrompt()
            HomeAlertsPrompts(signedIn)
            when {
                signedIn && (followsReady || followedChannels.isNotEmpty()) ->
                    HomeFollowedChannels(viewModel.follows, followAccess, scrollListToTop, open)
                recents.isEmpty() -> HomeEmptyStartFading(
                    shown = searchEntry == HomeSearchEntry.Field,
                    searchState = searchState,
                    channelInput = channelInput.value,
                    logInPrompt = logInPrompt,
                    onLogIn = onLogIn,
                )
                else -> HomeRecentChannels(viewModel, logInPrompt, scrollListToTop, open, onLogIn)
            }
        }
    }
    // Home's search opens on the channels, its categories a page away; a category opens in Explore.
    HomeChannelSearch(
        viewModel, searchState, searchEntry, channelInput, signedIn, SearchMode.Channels,
        categoryShowsUnderSearch = false, open, onOpenCategory, onSearchOpenChange,
    )
}

/** Opens a channel from Home: its stream when it is live, its profile otherwise. */
private fun homeChannelOpener(
    onWatch: (ChannelPreview) -> Unit,
    onOpenProfile: (ChannelPreview) -> Unit,
): (channel: ChannelPreview, live: Boolean) -> Unit = { channel, live ->
    when (homeChannelTarget(live)) {
        HomeChannelTarget.Stream -> onWatch(channel)
        HomeChannelTarget.Profile -> onOpenProfile(channel)
    }
}

/** The app's title, with the search button beside it while [searchShown]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTitleRow(searchState: SearchBarState, searchShown: Boolean) {
    // As tall as the search button, whether it shows or not, so the title stays put.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SEARCH_BUTTON_SIZE),
    ) {
        AppTitle(modifier = Modifier.weight(1f))
        AnimatedVisibility(visible = searchShown, enter = fadeIn(), exit = fadeOut()) {
            // Its icon lines up with the content's edge, the button's own padding hanging over the gutter.
            ChannelSearchButton(state = searchState, modifier = Modifier.offset(x = 12.dp))
        }
    }
}

/** The followed channels, pulled down to load the whole list again. */
@Composable
private fun ColumnScope.HomeFollowedChannels(
    follows: HomeFollows,
    access: suspend () -> TwitchUserAccess?,
    scrollToTop: OneShotRequest<Unit>?,
    onOpen: (channel: ChannelPreview, live: Boolean) -> Unit,
) {
    val channels by follows.channels.collectAsStateWithLifecycle()
    val fromCache by follows.fromCache.collectAsStateWithLifecycle()
    val status by follows.status.collectAsStateWithLifecycle()
    val refreshing by follows.refreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    Spacer(Modifier.height(SECTION_GAP))
    FollowedChannelsSection(
        channels = channels,
        fromCache = fromCache,
        status = status,
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                val current = access()
                if (current == null) {
                    follows.markUnavailable()
                } else {
                    follows.refresh(current.accessToken, current.userId, liveOnly = false)
                }
            }
        },
        onOpen = { channel, live ->
            focusManager.clearFocus()
            onOpen(channel, live)
        },
        scrollToTop = scrollToTop,
        modifier = Modifier.weight(1f),
    )
}

/** The search field and the log-in card of a Home without recents, fading in once it is [shown]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.HomeEmptyStartFading(
    shown: Boolean,
    searchState: SearchBarState,
    channelInput: String,
    logInPrompt: HomeLogInPromptState,
    onLogIn: () -> Unit,
) {
    // Starts hidden, so the field fades in when the last recent goes or the empty recents load.
    val emptyStartShown = remember { MutableTransitionState(false) }
    emptyStartShown.targetState = shown
    AnimatedVisibility(
        visibleState = emptyStartShown,
        modifier = Modifier.weight(1f),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        HomeEmptyStart(
            searchState = searchState,
            channelInput = channelInput,
            logInPrompt = logInPrompt,
            onLogIn = onLogIn,
        )
    }
}
