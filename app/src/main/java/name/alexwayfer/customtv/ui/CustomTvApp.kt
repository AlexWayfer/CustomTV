package name.alexwayfer.customtv.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.chat.SubForChatMode
import name.alexwayfer.customtv.ui.account.AccountNotice
import name.alexwayfer.customtv.ui.account.AccountLoginScreen
import name.alexwayfer.customtv.ui.account.AccountScreen
import name.alexwayfer.customtv.ui.account.ChannelAboutScreen
import name.alexwayfer.customtv.ui.account.PremiumVideoPlayer
import name.alexwayfer.customtv.ui.account.ProfileVideoPlayback
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.auth.playerYieldsToReloginPrompt
import name.alexwayfer.customtv.auth.resumeSavedChannel
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.FollowedChannelsRepository
import name.alexwayfer.customtv.data.OwnFollowRead
import name.alexwayfer.customtv.data.UserSubscriptionRead
import name.alexwayfer.customtv.ui.account.AccountViewModel
import name.alexwayfer.customtv.ui.account.playerExpandsAfterLoginCancelled
import name.alexwayfer.customtv.ui.account.TwitchLinkWarningDialog
import name.alexwayfer.customtv.ui.account.TwitchPermissionsDialog
import name.alexwayfer.customtv.ui.account.TwitchSessionEndedDialog
import name.alexwayfer.customtv.ui.account.openTwitchLinkSettings
import name.alexwayfer.customtv.ui.account.openTwitchLoginPage
import name.alexwayfer.customtv.ui.account.releaseLoginBrowser
import name.alexwayfer.customtv.ui.watch.ClearFocusWhenImeDismissed
import name.alexwayfer.customtv.ui.watch.ImeDismissFocus
import name.alexwayfer.customtv.ui.watch.LocalImeDismissFocus
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.ui.watch.SleepTimerEffect
import name.alexwayfer.customtv.ui.home.HomeScreen
import name.alexwayfer.customtv.ui.home.homeTapScrollsListToTop
import name.alexwayfer.customtv.data.StreamStartAlertsController
import name.alexwayfer.customtv.data.StreamAlertsLoginRequest
import name.alexwayfer.customtv.data.StreamStartRequest
import name.alexwayfer.customtv.data.WatchingChannel
import name.alexwayfer.customtv.ui.home.HomeViewModel
import name.alexwayfer.customtv.ui.home.followedChannelIds
import name.alexwayfer.customtv.ui.settings.SettingsScreen
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.whispers.WhispersBackground
import name.alexwayfer.customtv.ui.whispers.WhispersSection
import name.alexwayfer.customtv.update.AppUpdateOpenRequest
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.watch.WATCH_CHANNEL_ROUTE
import name.alexwayfer.customtv.ui.watch.WatchScreen
import name.alexwayfer.customtv.ui.watch.watchBackReturnsToPreviousChannel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTvApp() {
    val activity = LocalActivity.current as MainActivity
    val inPip by activity.inPictureInPicture.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val currentEntry by navController.currentBackStackEntryAsState()
    val pipResumePreferences = remember(activity) {
        activity.getSharedPreferences(PIP_RESUME_PREFERENCES, 0)
    }
    val watching = currentEntry?.destination?.route == WATCH_CHANNEL_ROUTE
    var player by rememberSaveable(stateSaver = PlayerSession.Saver) { mutableStateOf(PlayerSession()) }
    var channelLive by remember { mutableStateOf(false) }
    var wasInPip by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val rootView = LocalView.current
    var watchedThisSession by remember { mutableStateOf(false) }
    var pipResumeChecked by remember { mutableStateOf(false) }
    // Until the start decides whether it reopens the channel left in picture-in-picture.
    var startChannelPending by remember { mutableStateOf(true) }
    val accountViewModel: AccountViewModel = viewModel()
    val followsRepository = remember { FollowedChannelsRepository(BuildConfig.TWITCH_CLIENT_ID) }
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val accountNotice by accountViewModel.notice.collectAsStateWithLifecycle()
    val loginPrompt by accountViewModel.loginPrompt.collectAsStateWithLifecycle()
    val twitchLinkWarning by accountViewModel.twitchLinkWarning.collectAsStateWithLifecycle()
    val reloginForPermissions by accountViewModel.reloginForPermissions.collectAsStateWithLifecycle()
    val reloginForSession by accountViewModel.reloginForSession.collectAsStateWithLifecycle()
    val followsReady by accountViewModel.followsReady.collectAsStateWithLifecycle()
    val followedChannels by homeViewModel.followedChannels.collectAsStateWithLifecycle()
    val followedStatus by homeViewModel.followedStatus.collectAsStateWithLifecycle()
    val followedIds = remember(account != null, followedStatus, followedChannels) {
        followedChannelIds(account != null, followedStatus, followedChannels)
    }
    val loggingIn by accountViewModel.loggingIn.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var section by rememberSaveable { mutableStateOf(AppSection.Home) }
    var profileChannel by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsScrollTarget by remember { mutableStateOf<SettingsScrollTarget?>(null) }
    var profileOpenedFrom by rememberSaveable { mutableStateOf<AppSection?>(null) }
    var profileFirstTabRequest by remember { mutableIntStateOf(0) }
    var homeListToTop by remember { mutableStateOf<OneShotRequest<Unit>?>(null) }
    // Home leaves composition under another section; this keeps its scroll position for the way back.
    val homeState = rememberSaveableStateHolder()
    var awaitingLogin by rememberSaveable { mutableStateOf(false) }
    var returnToStreamAfterLogin by rememberSaveable { mutableStateOf(false) }
    val loginFailedMessage = stringResource(R.string.twitch_login_failed)
    val loginTimedOutMessage = stringResource(R.string.twitch_login_timed_out)
    val clientMissingMessage = stringResource(R.string.twitch_client_missing)
    val linksStillMessage = stringResource(R.string.twitch_links_still)
    var openPlayerFromHome by remember { mutableStateOf(true) }
    // One player is on screen at a time: PlayerSession closes the recording when a stream opens,
    // and the open decision closes every stream in the back stack when a recording opens.
    val openProfileVideo: (ProfileVideoPlayback) -> Unit = { video ->
        val decision = profileVideoOpenDecision(section, player.liveMinimized, watching)
        player = player.videoOpened(video, liveMinimized = decision.minimized)
        section = decision.section
        if (decision.closeLivePlayer) navController.popBackStack("home", inclusive = false)
    }
    val openChannel: (String, Boolean) -> Unit = { channel, fromHome ->
        AppLog.i(PLAYER_NAV_LOG_TAG, "open $channel, from home: $fromHome")
        openPlayerFromHome = fromHome
        player = player.liveOpened()
        homeViewModel.addRecent(channel)
        val currentChannel = currentEntry?.arguments?.getString("channel")
        if (!(watching && currentChannel.equals(channel, ignoreCase = true))) {
            navController.navigate("watch/$channel") {
                launchSingleTop = true
                popUpTo("home")
            }
        }
    }
    LaunchedEffect(reloginForPermissions, reloginForSession) {
        if (playerYieldsToReloginPrompt(reloginForPermissions, reloginForSession)) player = player.withLiveMinimized(true)
    }
    LaunchedEffect(account?.userId) {
        val signedIn = account?.userId != null
        StreamStartAlertsController.onSignedIn(signedIn)
    }
    ExperimentalLoginAccount(account?.userId)
    var sectionBeforeWhispers by remember { mutableStateOf(AppSection.Home) }
    var expandPlayerAfterWhispers by remember { mutableStateOf(false) }
    val openWhispers: () -> Unit = {
        if (section != AppSection.Whispers) {
            sectionBeforeWhispers = section
            expandPlayerAfterWhispers = player.fullScreenBeforeWhispers(watching)
        }
        player = player.whispersOpened(watching)
        section = AppSection.Whispers
    }
    val openWhispersLatest by rememberUpdatedState(openWhispers)
    WhispersBackground(userId = account?.userId, onOpenRequested = { openWhispersLatest() })
    LaunchedEffect(Unit) {
        StreamStartRequest.login.collect { login ->
            val channel = login ?: return@collect
            StreamStartRequest.clear()
            openChannel(channel, section == AppSection.Home)
        }
    }
    LaunchedEffect(Unit) {
        // Starting the login also shows the profile, where it finishes.
        StreamAlertsLoginRequest.requested.collect { requested ->
            if (!requested) return@collect
            StreamAlertsLoginRequest.clear()
            accountViewModel.logIn()
        }
    }
    val watchingLatest by rememberUpdatedState(watching)
    LaunchedEffect(Unit) {
        // A full-screen player shrinks to the mini player, so the settings are in sight.
        AppUpdateOpenRequest.requested.collect { requested ->
            if (!requested) return@collect
            AppUpdateOpenRequest.clear()
            player = player.coveringPlayerMinimized(watchingLatest)
            settingsScrollTarget = SettingsScrollTarget.Updates
            section = AppSection.Settings
        }
    }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeTargetBottomPx = WindowInsets.imeAnimationTarget.getBottom(density)
    var keepNavigationBarWhileImeCloses by remember { mutableStateOf(false) }
    var homeSearchOpen by remember { mutableStateOf(false) }
    val showNavigationBar = showAppNavigationBar(
        inPictureInPicture = inPip,
        imeBottomPx = imeBottomPx,
        imeTargetBottomPx = imeTargetBottomPx,
        keepVisibleWhileImeCloses = keepNavigationBarWhileImeCloses,
        keyboardInHomeSearch = homeSearchOpen,
    )
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx <= 0) keepNavigationBarWhileImeCloses = false
    }
    var fullPlayerCovering by remember { mutableStateOf(false) }
    var profileClosesUnderPlayer by remember { mutableStateOf(false) }
    SleepTimerEffect(snackbarHostState, inPictureInPicture = inPip) {
        AppLog.i(PLAYER_NAV_LOG_TAG, "close the players by the sleep timer, in picture-in-picture: $inPip")
        profileClosesUnderPlayer = false
        player = player.sleepTimerFired()
        navController.popBackStack("home", inclusive = false)
        // Picture-in-picture would stay open on the home screen; it leaves with the task instead.
        if (inPip) activity.moveTaskToBack(true)
    }
    val showShell = showAppShell(inPip, watching, player.liveMinimized, section)
    var navigationBarHeightPx by remember { mutableIntStateOf(0) }
    var navigationRailWidthPx by remember { mutableIntStateOf(0) }
    val navigationRail = appNavigationUsesRail(
        with(density) { LocalWindowInfo.current.containerSize.width.toDp() }.value,
    )
    // What the screens keep clear of: the bar's height at the bottom, or the rail's width at the start.
    val bottomBarShown = showNavigationBar && !navigationRail
    val railShown = showNavigationBar && navigationRail
    val bottomBarHeightPx = if (navigationRail) 0 else navigationBarHeightPx

    LaunchedEffect(currentEntry) {
        val entry = currentEntry ?: return@LaunchedEffect
        if (pipResumeChecked) return@LaunchedEffect
        pipResumeChecked = true
        val channelId = pipResumePreferences.getString(PIP_RESUME_CHANNEL_ID, null)
        pipResumePreferences.edit { remove(PIP_RESUME_CHANNEL_ID) }
        if (resumeSavedChannel(
                channelId,
                entry.destination.route == "home",
                reloginForPermissions || reloginForSession,
            )
        ) {
            val channel = channelId?.let { id -> ChannelAvatarRepository.refreshByIds(listOf(id)).firstOrNull()?.login }
            if (channel == null) {
                startChannelPending = false
                return@LaunchedEffect
            }
            AppLog.i(PLAYER_NAV_LOG_TAG, "reopen $channel left in picture-in-picture")
            player = player.liveOpened()
            navController.navigate("watch/$channel") {
                launchSingleTop = true
                popUpTo("home")
            }
        } else {
            startChannelPending = false
        }
    }
    LaunchedEffect(watching) {
        AppLog.i(PLAYER_NAV_LOG_TAG, if (watching) "stream screen shown" else "stream screen gone")
        // The reopened channel is on screen: the start has settled.
        if (watching) startChannelPending = false
    }

    DisposableEffect(rootView) {
        val wasFocusableInTouchMode = rootView.isFocusableInTouchMode
        rootView.isFocusableInTouchMode = true
        onDispose { rootView.isFocusableInTouchMode = wasFocusableInTouchMode }
    }

    LaunchedEffect(watching, player.liveClosedWithPip) {
        if (!watching || player.liveClosedWithPip) channelLive = false
        if (watching) {
            watchedThisSession = true
            return@LaunchedEffect
        }
        if (!watchedThisSession) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        focusManager.clearFocus(force = true)
        rootView.clearFocus()
    }

    LaunchedEffect(inPip, watching) {
        if (wasInPip && !inPip) {
            player = player.pipExpanded(watching)
        }
        wasInPip = inPip
    }
    // Closing picture-in-picture closes the recording in it. The stream binds its own handler.
    val recordingOpen = player.video != null
    DisposableEffect(activity, recordingOpen) {
        if (!recordingOpen) return@DisposableEffect onDispose { }
        val closeRecording: () -> Unit = { player = player.videoClosed() }
        activity.onPipClosed = closeRecording
        onDispose {
            if (activity.onPipClosed === closeRecording) activity.onPipClosed = null
        }
    }

    PremiumBanner(
        playerOpen = watching || recordingOpen,
        inPictureInPicture = inPip,
        onHome = section == AppSection.Home,
        startSettled = !startChannelPending,
    )

    val pipEnabled = pictureInPictureAllowed(
        watching = watching,
        channelLive = channelLive,
        liveClosedWithPip = player.liveClosedWithPip,
        recordingOpen = recordingOpen,
    )
    LaunchedEffect(pipEnabled) {
        activity.setPipEnabled(pipEnabled)
    }
    DisposableEffect(activity) {
        onDispose {
            activity.setPipEnabled(false)
        }
    }

    LaunchedEffect(accountNotice) {
        val notice = accountNotice ?: return@LaunchedEffect
        val message = when (notice) {
            AccountNotice.Failed -> loginFailedMessage
            AccountNotice.TimedOut -> loginTimedOutMessage
            AccountNotice.MissingClient -> clientMissingMessage
            AccountNotice.LinksStillClaimed -> linksStillMessage
        }
        accountViewModel.consumeNotice()
        snackbarHostState.showSnackbar(message)
    }
    // A login already running shows its loader again instead of doing nothing: the user may have left it for
    // another section while the browser took long, or after the page closed without telling the app.
    val logIn: () -> Unit = {
        accountViewModel.logIn()
        if (accountViewModel.loggingIn.value) section = AppSection.Account
    }
    var loginPageReopen by remember { mutableStateOf<OneShotRequest<Unit>?>(null) }
    LaunchedEffect(loggingIn) {
        if (loggingIn) {
            awaitingLogin = true
            section = AppSection.Account
        }
    }
    LaunchedEffect(account, awaitingLogin, loggingIn, twitchLinkWarning) {
        if (awaitingLogin && account != null) {
            awaitingLogin = false
            returnToStreamAfterLogin = false
            section = AppSection.Account
        } else if (awaitingLogin && !loggingIn && !twitchLinkWarning) {
            awaitingLogin = false
            if (playerExpandsAfterLoginCancelled(returnToStreamAfterLogin, watching && !player.liveClosedWithPip)) {
                player = player.withLiveMinimized(false)
            }
            returnToStreamAfterLogin = false
            section = AppSection.Home
        }
    }

    val imeDismissFocus = remember { ImeDismissFocus() }
    val gestureHints = rememberGestureHintReporter()
    gestureHints.Snackbars(snackbarHostState)
    val snackbarLift = remember { SnackbarLift() }
    CompositionLocalProvider(
        LocalImeDismissFocus provides imeDismissFocus,
        LocalUserAccess provides accountViewModel::userAccess,
        LocalGestureHints provides gestureHints,
        LocalSnackbarLift provides snackbarLift,
    ) {
    ClearFocusWhenImeDismissed()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TwitchBg),
    ) {
    val navigation = @Composable { modifier: Modifier ->
        AppNavigationBar(
            rail = navigationRail,
            section = section,
            account = account,
            onHome = {
                if (homeTapScrollsListToTop(section)) homeListToTop = OneShotRequest(Unit)
                section = AppSection.Home
            },
            onWhispers = openWhispers,
            onAccount = { section = AppSection.Account },
            onLogIn = logIn,
            onSettings = { section = AppSection.Settings },
            modifier = modifier,
        )
    }
    Row(Modifier.fillMaxSize()) {
    if (railShown) navigation(Modifier.onSizeChanged { navigationRailWidthPx = it.width })
    Column(Modifier.weight(1f).fillMaxHeight()) {
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (section == AppSection.Home || (watching && !player.liveMinimized && fullPlayerCovering)) {
            homeState.SaveableStateProvider(AppSection.Home) {
            HomeScreen(
                viewModel = homeViewModel,
                homeVisible = section == AppSection.Home && (!watching || (player.liveMinimized && !inPip)),
                signedIn = account != null,
                followsReady = followsReady,
                signedInUserId = account?.userId,
                followAccess = accountViewModel::userAccess,
                earlyFollowAccess = accountViewModel::freshSavedAccess,
                onFollowPermissionRejected = accountViewModel::rejectForNewPermissions,
                scrollListToTop = homeListToTop,
                padForIme = homeUsesImePadding(keepNavigationBarWhileImeCloses, homeSearchOpen),
                onSearchOpenChange = { homeSearchOpen = it },
                onWatch = { channel -> openChannel(channel, true) },
                onOpenProfile = { channel ->
                    homeViewModel.addRecent(channel)
                    profileChannel = channel
                    profileOpenedFrom = AppSection.Home
                    section = AppSection.ChannelProfile
                },
                onLogIn = logIn,
            )
            }
        }
    }
        if (bottomBarShown) navigation(Modifier.onSizeChanged { navigationBarHeightPx = it.height })
    }
    }
        if (watching && !player.liveMinimized && !inPip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            )
        }
        val screenWidthPx = LocalWindowInfo.current.containerSize.width
        val miniPlayerBottom = if (bottomBarShown) {
            with(density) { navigationBarHeightPx.toDp() } + 12.dp
        } else {
            12.dp
        }
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = when {
                !watching -> Modifier.size(0.dp)
                inPip || !player.liveMinimized -> Modifier.fillMaxSize()
                else -> Modifier
                    .align(Alignment.BottomEnd)
                    .wrapContentSize(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = miniPlayerBottom)
                    .offscreenWhen(miniPlayerHidden(section, minimized = true, inPictureInPicture = false), screenWidthPx)
            }.zIndex(1f),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(route = "home") {}
            composable(
                route = WATCH_CHANNEL_ROUTE,
                arguments = listOf(navArgument("channel") { type = NavType.StringType }),
                enterTransition = {
                    if (openPlayerFromHome) {
                        fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 8 }
                    } else {
                        EnterTransition.None
                    }
                },
                popExitTransition = { ExitTransition.None },
            ) { entry ->
                val channel = entry.arguments?.getString("channel").orEmpty()
                DisposableEffect(channel, player.liveClosedWithPip) {
                    WatchingChannel.login = if (player.liveClosedWithPip || channel.isBlank()) null else channel
                    onDispose {
                        if (WatchingChannel.login.equals(channel, ignoreCase = true)) {
                            WatchingChannel.login = null
                        }
                    }
                }
                val chatViewModel: ChatViewModel = viewModel()
                BindPipCloseHandlers(
                    activity = activity,
                    channel = channel,
                    preferences = pipResumePreferences,
                    chatViewModel = chatViewModel,
                    onClosed = { player = player.livePipClosed() },
                    onResumed = { player = player.livePipResumed() },
                )
                if (!player.liveClosedWithPip) {
                    WatchScreen(
                        channel = channel,
                        chatViewModel = chatViewModel,
                        inPictureInPicture = inPip,
                        minimized = player.liveMinimized && !inPip,
                        appNavigationBarHeightPx = bottomBarHeightPx,
                        signedIn = account != null,
                        signedInUserId = account?.userId,
                        signedInLogin = account?.login,
                        signedInDisplayName = account?.displayName,
                        raidToken = { accountViewModel.userAccess()?.accessToken },
                        ownChatterFollow = { broadcasterId ->
                            val access = accountViewModel.userAccess()
                            if (access == null) {
                                null
                            } else {
                                followsRepository.ownChatterFollow(access.accessToken, access.userId, broadcasterId)
                            }
                        },
                        readOwnFollow = { broadcasterId ->
                            val access = accountViewModel.userAccess()
                            if (access == null) {
                                FollowForChatMode.Unknown
                            } else {
                                when (val read = followsRepository.readOwnFollow(
                                    access.accessToken,
                                    access.userId,
                                    broadcasterId,
                                )) {
                                    is OwnFollowRead.Following -> FollowForChatMode.Following(read.atMillis)
                                    OwnFollowRead.NotFollowing -> FollowForChatMode.NotFollowing
                                    OwnFollowRead.Unavailable -> FollowForChatMode.Unknown
                                }
                            }
                        },
                        readOwnSubscription = { broadcasterId ->
                            val access = accountViewModel.userAccess()
                            if (access == null) {
                                SubForChatMode.Unknown
                            } else {
                                when (followsRepository.readOwnSubscription(
                                    access.accessToken,
                                    access.userId,
                                    broadcasterId,
                                )) {
                                    UserSubscriptionRead.Subscribed -> SubForChatMode.Subscribed
                                    UserSubscriptionRead.NotSubscribed -> SubForChatMode.NotSubscribed
                                    UserSubscriptionRead.Unavailable -> SubForChatMode.Unknown
                                }
                            }
                        },
                        followedChannelIds = followedIds,
                        onFollowsChanged = { homeViewModel.reloadFollowsAfterChange(accountViewModel::userAccess) },
                        onOpenChannel = { target ->
                            AppLog.i(PLAYER_NAV_LOG_TAG, "open $target from the stream")
                            openPlayerFromHome = fullPlayerCovering
                            player = player.withLiveMinimized(false)
                            homeViewModel.addRecent(target)
                            val currentChannel = currentEntry?.arguments?.getString("channel")
                            if (!currentChannel.equals(target, ignoreCase = true)) {
                                navController.navigate("watch/$target")
                            }
                        },
                        onOpenChannelProfile = { target ->
                            profileChannel = target
                            profileOpenedFrom = null
                            section = AppSection.ChannelProfile
                        },
                        onOpenSettings = { target ->
                            settingsScrollTarget = target
                            section = AppSection.Settings
                        },
                        onMinimizeOverSection = { player = player.withLiveMinimized(true) },
                        returnsToPreviousChannel = watchBackReturnsToPreviousChannel(
                            navController.previousBackStackEntry?.destination?.route,
                        ),
                        onReturnToPreviousChannel = {
                            player = player.withLiveMinimized(false)
                            navController.popBackStack()
                        },
                        onMinimize = {
                            AppLog.i(PLAYER_NAV_LOG_TAG, "minimize")
                            profileClosesUnderPlayer = false
                            section = sectionAfterPlayerMinimizes(section, watching, player.liveMinimized)
                            player = player.withLiveMinimized(true)
                        },
                        onFullPlayerCovering = {
                            fullPlayerCovering = true
                            section = sectionBehindOpenPlayer(section, playerCovering = true)
                        },
                        onFullPlayerExpanded = {
                            section = sectionAfterPlayerExpands(section, profileClosesUnderPlayer)
                            profileClosesUnderPlayer = false
                        },
                        onLogIn = {
                            accountViewModel.logIn()
                            if (accountViewModel.loggingIn.value) {
                                returnToStreamAfterLogin = watching
                                player = player.withLiveMinimized(true)
                                section = AppSection.Account
                            }
                        },
                        onMinimizeDragDismissesKeyboard = { keepNavigationBarWhileImeCloses = true },
                        onExpand = {
                            AppLog.i(PLAYER_NAV_LOG_TAG, "expand")
                            player = player.withLiveMinimized(false)
                        },
                        onLiveStatus = { channelLive = it },
                        onClose = {
                            AppLog.i(PLAYER_NAV_LOG_TAG, "close the stream")
                            profileClosesUnderPlayer = false
                            player = player.withLiveMinimized(false)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
        // The last registered Back wins. After NavHost, so its pop of the stream does not close the
        // mini player; before the sections, so a section's own Back still comes first.
        BackHandler(
            enabled = homeBackKeepsMiniPlayer(
                section = section,
                liveMiniPlayer = watching && player.liveMinimized && !player.liveClosedWithPip,
                videoMiniPlayer = recordingOpen && player.videoMinimized,
                inPictureInPicture = inPip,
            ),
        ) {
            if (activity.enterPip()) return@BackHandler
            // Without picture-in-picture the mini player closes, as Back did before.
            if (recordingOpen) {
                player = player.videoClosed()
            } else {
                AppLog.i(PLAYER_NAV_LOG_TAG, "close the mini player by Back")
                player = player.withLiveMinimized(false)
                navController.popBackStack()
            }
        }
        val signedIn = account
        if (showShell) {
            AnimatedContent(
                targetState = section,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (railShown) with(density) { navigationRailWidthPx.toDp() } else 0.dp,
                        bottom = if (bottomBarShown) {
                            with(density) { navigationBarHeightPx.toDp() }
                        } else {
                            0.dp
                        },
                    )
                    // The app's bar covers the system's, so a screen's own Scaffold must not keep clear of it again.
                    .then(if (bottomBarShown) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
                    .then(
                        if (section == AppSection.Home) {
                            Modifier.clearAndSetSemantics {}
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.TopStart,
                transitionSpec = {
                    if (targetState == AppSection.ChannelProfile) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        ContentTransform(
                            fadeIn(tween(200)),
                            fadeOut(tween(200)),
                            sizeTransform = null,
                        )
                    }
                },
                label = "app section",
            ) { page ->
                if (page != AppSection.Home) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(TwitchBg),
                    ) {
                        if (page == AppSection.Whispers) {
                            WhispersSection(
                                onClosed = {
                                    section = sectionBeforeWhispers
                                    if (expandPlayerAfterWhispers) player = player.returnedFromWhispers(watching)
                                    expandPlayerAfterWhispers = false
                                },
                                onOpenChannel = { channel ->
                                    profileChannel = channel
                                    profileOpenedFrom = AppSection.Whispers
                                    section = AppSection.ChannelProfile
                                },
                            )
                        }
                        val accountPage = accountSectionPage(loggingIn, signedIn = signedIn != null)
                        if (page == AppSection.Account && accountPage == AccountSectionPage.LoginLoader) {
                            AccountLoginScreen(
                                pageReady = loginPrompt != null,
                                onReopen = { loginPageReopen = OneShotRequest(Unit) },
                                onCancel = accountViewModel::cancelLogin,
                            )
                        }
                        if (page == AppSection.Account && accountPage == AccountSectionPage.Profile && signedIn != null) {
                            AccountScreen(
                                account = signedIn,
                                onLeave = { section = AppSection.Home },
                                onLogOut = {
                                    accountViewModel.logOut()
                                    section = AppSection.Home
                                },
                                onOpenChannel = { channel -> openChannel(channel, false) },
                                onOpenVideo = openProfileVideo,
                            )
                        }
                        if (page == AppSection.ChannelProfile) {
                            profileChannel?.let { channel ->
                                ChannelAboutScreen(
                                    channel = channel,
                                    // Whispers go to another person's channel, from a logged-in account.
                                    canWhisper = account != null && !channel.equals(account?.login, ignoreCase = true),
                                    backEnabled = profileHandlesBack(watching, player.liveMinimized),
                                    onLeave = {
                                        when (val back = profileBack(profileOpenedFrom, watching, player.liveMinimized)) {
                                            ProfileBack.ExpandPlayer -> {
                                                profileClosesUnderPlayer = true
                                                player = player.withLiveMinimized(false)
                                            }
                                            is ProfileBack.Show -> section = back.section
                                        }
                                        profileOpenedFrom = null
                                    },
                                    onOpenChannel = { target -> openChannel(target, false) },
                                    onOpenVideo = openProfileVideo,
                                    firstTabRequest = profileFirstTabRequest,
                                )
                            }
                        }
                        if (page == AppSection.Settings) {
                            SettingsScreen(
                                onClose = { section = AppSection.Home },
                                signedIn = account != null,
                                accountName = account?.displayName,
                                scrollTo = settingsScrollTarget,
                                onScrolled = { settingsScrollTarget = null },
                            )
                        }
                    }
                }
            }
        }
        PremiumVideoPlayer(
            playback = player.video,
            onClose = { player = player.videoClosed() },
            onOpenChannelProfile = { channel ->
                profileChannel = channel
                profileOpenedFrom = null
                profileFirstTabRequest++
                section = AppSection.ChannelProfile
            },
            minimized = player.videoMinimized,
            onMinimize = { player = player.withVideoMinimized(true) },
            onExpand = { player = player.withVideoMinimized(false) },
            appNavigationBarHeightPx = bottomBarHeightPx,
            inPictureInPicture = inPip,
            selfLogin = account?.login.orEmpty(),
            selfDisplayName = account?.displayName.orEmpty(),
            modifier = if (player.videoMinimized && !inPip) {
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = miniPlayerBottom)
                    .offscreenWhen(miniPlayerHidden(section, minimized = true, inPictureInPicture = false), screenWidthPx)
                    .zIndex(2f)
            } else {
                Modifier.fillMaxSize().zIndex(2f)
            },
        )
        val prompt = loginPrompt
        val loginSheetScreenPx = LocalWindowInfo.current.containerSize.height
        if (prompt != null) {
            DisposableEffect(prompt.verificationUri) {
                onDispose { releaseLoginBrowser(activity) }
            }
            val openLoginPage = {
                openTwitchLoginPage(
                    activity,
                    prompt.verificationUri,
                    loginSheetScreenPx,
                    onHidden = accountViewModel::loginPageClosed,
                )
            }
            LaunchedEffect(prompt.verificationUri) { openLoginPage() }
            LaunchedEffect(loginPageReopen) {
                loginPageReopen?.take() ?: return@LaunchedEffect
                openLoginPage()
            }
        }
        if (twitchLinkWarning) {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) accountViewModel.refreshTwitchLinks()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            TwitchLinkWarningDialog(
                onOpenSettings = { openTwitchLinkSettings(activity) },
                onDismiss = { accountViewModel.dismissTwitchLinkWarning() },
            )
        }
        if (reloginForPermissions) {
            TwitchPermissionsDialog(
                onLogIn = { accountViewModel.confirmScopeRelogin() },
                onDismiss = { accountViewModel.dismissPermissionsRelogin() },
            )
        } else if (reloginForSession) {
            TwitchSessionEndedDialog(
                onLogIn = { accountViewModel.confirmSessionRelogin() },
                onDismiss = { accountViewModel.dismissSessionRelogin() },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { AppSnackbar(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    // The full screen player covers the rail.
                    start = if (railShown && !snackbarLift.overPlayer) {
                        with(density) { navigationRailWidthPx.toDp() }
                    } else {
                        0.dp
                    },
                    bottom = if (bottomBarShown) {
                        with(density) { navigationBarHeightPx.toDp() }
                    } else {
                        0.dp
                    },
                )
                // Without the app's bar, as over the player, it keeps clear of the system's.
                .then(if (bottomBarShown) Modifier else Modifier.navigationBarsPadding())
                .then(if (snackbarLift.overPlayer) Modifier.widthIn(max = SnackbarOverPlayerMaxWidth) else Modifier)
                .offset { IntOffset(0, -snackbarLift.px()) }
                // Above the players, so a tip or a notice shows over the stream too.
                .zIndex(3f),
        )
    }
    }
}

private const val PIP_RESUME_PREFERENCES = "pip_resume"
internal const val PIP_RESUME_CHANNEL_ID = "channel"
private const val PLAYER_NAV_LOG_TAG = "PlayerNav"
