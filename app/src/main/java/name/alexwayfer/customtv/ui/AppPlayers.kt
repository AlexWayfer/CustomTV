package name.alexwayfer.customtv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.data.WatchingChannel
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.account.PremiumVideoPlayer
import name.alexwayfer.customtv.ui.explore.exploreNavEnabled
import name.alexwayfer.customtv.ui.watch.WATCH_CHANNEL_ROUTE
import name.alexwayfer.customtv.ui.watch.WatchAccount
import name.alexwayfer.customtv.ui.watch.WatchNavigation
import name.alexwayfer.customtv.ui.watch.WatchPlacement
import name.alexwayfer.customtv.ui.watch.WatchScreen
import name.alexwayfer.customtv.ui.watch.closeWatchedChannels
import name.alexwayfer.customtv.ui.watch.watchBackReturnsToPreviousChannel
import name.alexwayfer.customtv.ui.watch.watchChannelArguments
import name.alexwayfer.customtv.ui.watch.watchChannelPreview
import name.alexwayfer.customtv.ui.watch.watchRoute

/** The stream's route: full screen, in the corner, or out of sight while no stream is open. */
@Composable
internal fun BoxScope.AppPlayerHost(scene: AppScene) {
    val shell = scene.shell
    val player = shell.player
    NavHost(
        navController = scene.navController,
        startDestination = "home",
        modifier = when {
            !scene.watching -> Modifier.size(0.dp)
            scene.inPip || !player.liveMinimized -> Modifier.fillMaxSize()
            else -> Modifier
                .align(Alignment.BottomEnd)
                .wrapContentSize(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = scene.layout.miniPlayerBottom)
                .offscreenWhen(
                    miniPlayerHidden(shell.section, minimized = true, inPictureInPicture = false),
                    scene.layout.screenWidthPx,
                )
        }.zIndex(1f),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(route = "home") {}
        composable(
            route = WATCH_CHANNEL_ROUTE,
            arguments = watchChannelArguments,
            enterTransition = {
                if (shell.openPlayerFromHome) {
                    fadeIn(tween(PLAYER_FROM_HOME_FADE_MS)) +
                        slideInVertically(tween(PLAYER_FROM_HOME_SLIDE_MS)) { (it * PLAYER_FROM_HOME_RISE).toInt() }
                } else {
                    EnterTransition.None
                }
            },
            popExitTransition = { ExitTransition.None },
        ) { entry ->
            WatchRoute(scene, entry)
        }
    }
}

@Composable
private fun WatchRoute(scene: AppScene, entry: NavBackStackEntry) {
    val shell = scene.shell
    val preview = watchChannelPreview(entry.arguments)
    val channel = preview.login
    DisposableEffect(channel, shell.player.liveClosedWithPip) {
        WatchingChannel.login = if (shell.player.liveClosedWithPip || channel.isBlank()) null else channel
        onDispose {
            if (WatchingChannel.login.equals(channel, ignoreCase = true)) {
                WatchingChannel.login = null
            }
        }
    }
    val chatViewModel: ChatViewModel = viewModel()
    BindPipCloseHandlers(
        activity = scene.activity,
        channel = channel,
        preferences = scene.pipResumePreferences,
        chatViewModel = chatViewModel,
        onClosed = { shell.player = shell.player.livePipClosed() },
        onResumed = { shell.player = shell.player.livePipResumed() },
    )
    if (shell.player.liveClosedWithPip) return
    WatchScreen(
        channel = channel,
        preview = preview,
        chatViewModel = chatViewModel,
        placement = WatchPlacement(
            inPictureInPicture = scene.inPip,
            minimized = shell.player.liveMinimized && !scene.inPip,
            appNavigationBarHeightPx = scene.layout.bottomBarHeightPx,
        ),
        account = scene.watchAccount(),
        navigation = scene.watchNavigation(),
    )
}

private fun AppScene.watchAccount(): WatchAccount {
    val account = account
    return WatchAccount(
        signedIn = account != null,
        userId = account?.userId,
        login = account?.login,
        displayName = account?.displayName,
        raidToken = { accountViewModel.session.userAccess()?.accessToken },
        ownChatterFollow = ::ownChatterFollow,
        readOwnFollow = ::readOwnFollow,
        readOwnSubscription = ::readOwnSubscription,
        followedChannelIds = followedIds,
        onFollowsChanged = { homeViewModel.follows.reload(accountViewModel.session::userAccess) },
        onLogIn = {
            accountViewModel.logIn()
            if (accountViewModel.loggingIn.value) {
                shell.returnToStreamAfterLogin = watching
                shell.player = shell.player.withLiveMinimized(true)
                shell.section = AppSection.Account
            }
        },
    )
}

private fun AppScene.watchNavigation(): WatchNavigation = WatchNavigation(
    onOpenChannel = { target ->
        AppLog.i(PLAYER_NAV_LOG_TAG, "open ${target.login} from the stream")
        shell.openPlayerFromHome = shell.fullPlayerCovering
        shell.player = shell.player.withLiveMinimized(false)
        homeViewModel.addRecent(target.login)
        val currentChannel = currentEntry.value?.arguments?.getString("channel")
        if (!currentChannel.equals(target.login, ignoreCase = true)) {
            navController.navigate(watchRoute(target))
        }
    },
    onOpenChannelProfile = { target ->
        shell.profileChannel = target
        shell.profileOpenedFrom = null
        shell.section = AppSection.ChannelProfile
    },
    onOpenSettings = { target ->
        shell.settingsScrollTarget = target
        shell.section = AppSection.Settings
    },
    onOpenCategory = if (exploreNavEnabled()) this::openCategoryFromPlayer else null,
    onMinimizeOverSection = { shell.player = shell.player.withLiveMinimized(true) },
    returnsToPreviousChannel = watchBackReturnsToPreviousChannel(
        navController.previousBackStackEntry?.destination?.route,
    ),
    onReturnToPreviousChannel = {
        shell.player = shell.player.withLiveMinimized(false)
        navController.popBackStack()
    },
    onMinimize = {
        AppLog.i(PLAYER_NAV_LOG_TAG, "minimize")
        shell.profileClosesUnderPlayer = false
        shell.exploreCategoryClosesUnderPlayer = false
        shell.section = sectionAfterPlayerMinimizes(shell.section, watching, shell.player.liveMinimized)
        shell.player = shell.player.withLiveMinimized(true)
    },
    onFullPlayerCovering = {
        shell.fullPlayerCovering = true
        shell.section = sectionBehindOpenPlayer(shell.section, playerCovering = true)
    },
    onFullPlayerExpanded = {
        shell.section = sectionAfterPlayerExpands(shell.section, shell.profileClosesUnderPlayer)
        shell.profileClosesUnderPlayer = false
        closeExploreCategoryUnderPlayer()
    },
    onMinimizeDragDismissesKeyboard = { shell.keepNavigationBarWhileImeCloses = true },
    onExpand = {
        AppLog.i(PLAYER_NAV_LOG_TAG, "expand")
        shell.player = shell.player.withLiveMinimized(false)
    },
    onLiveStatus = { shell.channelLive = it },
    onClose = {
        AppLog.i(PLAYER_NAV_LOG_TAG, "close the stream")
        shell.profileClosesUnderPlayer = false
        shell.exploreCategoryClosesUnderPlayer = false
        shell.player = shell.player.withLiveMinimized(false)
        navController.closeWatchedChannels()
    },
)

/**
 * The last registered Back wins. After NavHost, so its pop of the stream does not close the
 * mini player; before the sections, so a section's own Back still comes first.
 */
@Composable
internal fun AppMiniPlayerBack(scene: AppScene) {
    val shell = scene.shell
    val player = shell.player
    BackHandler(
        enabled = homeBackKeepsMiniPlayer(
            section = shell.section,
            liveMiniPlayer = scene.watching && player.liveMinimized && !player.liveClosedWithPip,
            videoMiniPlayer = scene.recordingOpen && player.videoMinimized,
            inPictureInPicture = scene.inPip,
        ),
    ) {
        if (scene.activity.enterPip()) return@BackHandler
        // Without picture-in-picture the mini player closes, as Back did before.
        if (scene.recordingOpen) {
            shell.player = shell.player.videoClosed()
        } else {
            AppLog.i(PLAYER_NAV_LOG_TAG, "close the mini player by Back")
            shell.player = shell.player.withLiveMinimized(false)
            scene.navController.closeWatchedChannels()
        }
    }
}

/** A recording from a profile, over everything, or in the corner like the stream's mini player. */
@Composable
internal fun BoxScope.AppRecordingPlayer(scene: AppScene) {
    val shell = scene.shell
    val player = shell.player
    PremiumVideoPlayer(
        playback = player.video,
        onClose = {
            shell.exploreCategoryClosesUnderPlayer = false
            shell.player = shell.player.videoClosed()
        },
        onOpenChannelProfile = { channel ->
            shell.profileChannel = channel
            shell.profileOpenedFrom = null
            shell.profileFirstTabRequest++
            shell.section = AppSection.ChannelProfile
        },
        onOpenCategory = scene::openCategoryFromPlayer,
        minimized = player.videoMinimized,
        onMinimize = {
            shell.exploreCategoryClosesUnderPlayer = false
            shell.player = shell.player.withVideoMinimized(true)
        },
        onExpand = { shell.player = shell.player.withVideoMinimized(false) },
        onExpanded = scene::closeExploreCategoryUnderPlayer,
        onBack = player.videoBackReturnChannel?.let { channel -> { scene.returnFromVideoToStream(channel) } },
        appNavigationBarHeightPx = scene.layout.bottomBarHeightPx,
        inPictureInPicture = scene.inPip,
        selfLogin = scene.account?.login.orEmpty(),
        selfDisplayName = scene.account?.displayName.orEmpty(),
        selfUserId = scene.account?.userId,
        modifier = if (player.videoMinimized && !scene.inPip) {
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = scene.layout.miniPlayerBottom)
                .offscreenWhen(
                    miniPlayerHidden(shell.section, minimized = true, inPictureInPicture = false),
                    scene.layout.screenWidthPx,
                )
                .zIndex(2f)
        } else {
            Modifier.fillMaxSize().zIndex(2f)
        },
    )
}

// A stream opened from Home fades in while it rises from an eighth of its height below.
private const val PLAYER_FROM_HOME_FADE_MS = 220
private const val PLAYER_FROM_HOME_SLIDE_MS = 280
private const val PLAYER_FROM_HOME_RISE = 1f / 8f
