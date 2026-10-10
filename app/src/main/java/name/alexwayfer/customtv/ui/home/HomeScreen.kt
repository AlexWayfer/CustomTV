package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.isValidChannelLogin
import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.ui.account.TwitchUserAccess
import name.alexwayfer.customtv.ui.components.AppTitle
import name.alexwayfer.customtv.ui.notifications.HomeNotificationPrompt
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import kotlin.time.Duration.Companion.milliseconds

private const val RECENTS_HEADER_KEY = "recents-header"
private const val LOG_IN_PROMPT_KEY = "log-in-prompt"

/** From the title row to the title of the list below it. */
private val SECTION_GAP = 16.dp

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
    onWatch: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onLogIn: () -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    padForIme: Boolean = true,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val recentsRestored by viewModel.recentsRestored.collectAsStateWithLifecycle()
    val checkingChannel by viewModel.checkingChannel.collectAsStateWithLifecycle()
    val refreshingRecents by viewModel.refreshingRecents.collectAsStateWithLifecycle()
    val recentProfiles by viewModel.recentProfiles.collectAsStateWithLifecycle()
    val channelSearch by viewModel.channelSearch.collectAsStateWithLifecycle()
    val followedChannels by viewModel.followedChannels.collectAsStateWithLifecycle()
    val followedFromCache by viewModel.followedFromCache.collectAsStateWithLifecycle()
    val followedStatus by viewModel.followedStatus.collectAsStateWithLifecycle()
    val refreshingFollows by viewModel.refreshingFollows.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val searchState = rememberSearchBarState()
    val searchEntry = homeSearchEntry(signedIn, recentsRestored, recents.isNotEmpty())
    val logInPrompt = rememberHomeLogInPrompt(signedIn)
    var channelInput by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val invalidMessage = stringResource(R.string.invalid_channel)
    val missingMessage = stringResource(R.string.channel_not_found)
    val lookupFailedMessage = stringResource(R.string.channel_lookup_failed)
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var wasInForeground by remember(lifecycleOwner) { mutableStateOf(false) }
    var firstHomeRefresh by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            appInForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(homeVisible, recents) {
        if (homeVisible) viewModel.rememberCachedProfiles()
    }

    // The search opens in its own window, which would stay over a player or another screen covering Home.
    LaunchedEffect(homeVisible) {
        if (!homeVisible) searchState.snapTo(0f)
    }

    LaunchedEffect(homeVisible, appInForeground, recents, signedIn) {
        val returnedToForeground = appInForeground && !wasInForeground
        wasInForeground = appInForeground
        if (!homeVisible) firstHomeRefresh = false
        if (!homeVisible || !appInForeground || !refreshRecentsInBackground(signedIn, recents.isNotEmpty())) {
            return@LaunchedEffect
        }
        viewModel.refreshRecents(
            showProgress = showAutomaticRecentRefreshProgress(firstHomeRefresh, returnedToForeground),
            automatic = true,
        )
        firstHomeRefresh = false
        while (isActive) {
            delay(RECENT_AUTO_REFRESH_INTERVAL_MS.milliseconds)
            viewModel.refreshRecents(showProgress = false, automatic = true)
        }
    }

    LaunchedEffect(signedIn, followedStatus) {
        if (signedIn && followedStatus == FollowedChannelsStatus.NeedsPermission) {
            onFollowPermissionRejected()
        }
    }

    LaunchedEffect(signedIn, signedInUserId, homeVisible, appInForeground) {
        if (!signedIn || !homeVisible || !appInForeground) return@LaunchedEffect
        val early = earlyFollowAccess()
        if (early != null) {
            viewModel.refreshFollows(early.accessToken, early.userId, liveOnly = false, automatic = true)
        }
        val access = followAccess()
        if (access == null) {
            if (viewModel.followedChannels.value.isEmpty()) viewModel.markFollowsUnavailable()
            return@LaunchedEffect
        }
        if (early == null) {
            viewModel.refreshFollows(access.accessToken, access.userId, liveOnly = false, automatic = true)
        }
        while (isActive) {
            delay(FOLLOWED_AUTO_REFRESH_INTERVAL_MS.milliseconds)
            val status = viewModel.followedStatus.value
            if (
                status == FollowedChannelsStatus.NeedsPermission ||
                status == FollowedChannelsStatus.Rejected
            ) {
                break
            }
            val next = followAccess() ?: break
            viewModel.refreshFollows(next.accessToken, next.userId, liveOnly = true)
        }
    }

    fun open(login: String, live: Boolean) {
        when (homeChannelTarget(live)) {
            HomeChannelTarget.Stream -> onWatch(login)
            HomeChannelTarget.Profile -> onOpenProfile(login)
        }
    }

    fun submit(raw: String = channelInput) {
        if (checkingChannel) return
        val login = normalizeChannelInput(raw)
        if (!isValidChannelLogin(login)) {
            error = invalidMessage
            return
        }
        error = null
        focusManager.clearFocus()
        viewModel.openTypedChannel(login) { result ->
            when (result) {
                is ChannelLookup.Found -> {
                    channelInput = channelInputAfterLookup(channelInput, result)
                    viewModel.updateChannelSearch(channelInput)
                    // Closed before Home goes: an animation would be cancelled with it, and the open search
                    // would be saved and come back over the next screen that shows Home again.
                    scope.launch {
                        searchState.snapTo(0f)
                        open(result.profile.login, result.profile.isLive)
                    }
                }
                ChannelLookup.NotFound -> error = missingMessage
                ChannelLookup.Unavailable -> error = lookupFailedMessage
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TwitchBg)
            .statusBarsPadding()
            .then(if (padForIme) Modifier.imePadding() else Modifier)
            .padding(horizontal = 16.dp),
    ) {
        // As tall as the search button, whether it shows or not, so the title stays put.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SEARCH_BUTTON_SIZE),
        ) {
            AppTitle(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = searchEntry == HomeSearchEntry.Button,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                // Its icon lines up with the content's edge, the button's own padding hanging over the gutter.
                ChannelSearchButton(state = searchState, modifier = Modifier.offset(x = 12.dp))
            }
        }
        HomeNotificationPrompt()
        if (signedIn && (followsReady || followedChannels.isNotEmpty())) {
            Spacer(Modifier.height(SECTION_GAP))
            FollowedChannelsSection(
                channels = followedChannels,
                fromCache = followedFromCache,
                status = followedStatus,
                refreshing = refreshingFollows,
                onRefresh = {
                    scope.launch {
                        val access = followAccess()
                        if (access == null) {
                            viewModel.markFollowsUnavailable()
                        } else {
                            viewModel.refreshFollows(access.accessToken, access.userId, liveOnly = false)
                        }
                    }
                },
                onOpen = { login, live ->
                    focusManager.clearFocus()
                    open(login, live)
                },
                scrollToTop = scrollListToTop,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        if (recents.isEmpty()) {
            // Starts hidden, so the field fades in when the last recent goes or the empty recents load.
            val emptyStartShown = remember { MutableTransitionState(false) }
            emptyStartShown.targetState = searchEntry == HomeSearchEntry.Field
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
        } else {
            Spacer(Modifier.height(SECTION_GAP))
            var headerHeightPx by remember { mutableIntStateOf(0) }
            val listState = rememberLazyListState()
            // A channel newly on top scrolls up to it; coming back to Home with the same list keeps the place.
            var shownFirstRecent by rememberSaveable { mutableStateOf<String?>(null) }
            LaunchedEffect(recents.firstOrNull()?.id) {
                val firstId = recents.firstOrNull()?.id
                if (firstId != null && firstId != shownFirstRecent) listState.scrollToItem(0)
                shownFirstRecent = firstId
            }
            LaunchedEffect(scrollListToTop) {
                if (scrollListToTop?.take() != null) listState.animateScrollToItem(0)
            }
            ListPullToRefreshBox(
                isRefreshing = refreshingRecents,
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
                                open(channel.login, live)
                            },
                            onRemove = { viewModel.removeRecent(channel) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
    }
    ChannelSearchExpanded(
        state = searchState,
        collapsedShape = if (searchEntry == HomeSearchEntry.Field) ChannelSearchFieldShape else ChannelSearchButtonShape,
        value = channelInput,
        onValueChange = {
            channelInput = it
            error = null
            viewModel.updateChannelSearch(it)
        },
        history = recents.map { it.login },
        profiles = recentProfiles,
        followed = if (signedIn) followedChannels else emptyList(),
        searchHits = channelSearch,
        liveLogins = followedChannels.mapNotNullTo(mutableSetOf()) { channel ->
            channel.login.lowercase().takeIf { channel.isLive }
        },
        error = error,
        checking = checkingChannel,
        onSubmit = { submit() },
        onPick = { login ->
            channelInput = login
            submit(login)
        },
        onExpandedChange = { expanded ->
            onSearchOpenChange(expanded)
            if (!expanded) error = null
            if (refreshRecentsOnSearchOpen(signedIn, expanded, recents.isNotEmpty())) {
                viewModel.refreshRecents(showProgress = false, automatic = true)
            }
        },
    )
}
