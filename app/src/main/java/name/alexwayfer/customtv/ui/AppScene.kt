package name.alexwayfer.customtv.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.chat.ChatterFollow
import name.alexwayfer.customtv.chat.FollowForChatMode
import name.alexwayfer.customtv.chat.SubForChatMode
import name.alexwayfer.customtv.data.OwnChannelReads
import name.alexwayfer.customtv.data.OwnFollowRead
import name.alexwayfer.customtv.data.UserSubscriptionRead
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.account.AccountViewModel
import name.alexwayfer.customtv.ui.account.DeviceLoginPrompt
import name.alexwayfer.customtv.ui.account.ProfileVideoPlayback
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.home.HomeViewModel
import name.alexwayfer.customtv.ui.home.followedChannelIds
import name.alexwayfer.customtv.ui.watch.closeWatchedChannels
import name.alexwayfer.customtv.ui.watch.watchRoute

/** The signed-in account and the login under way, as the account view model streams them. */
internal data class AppAccount(
    val account: TwitchAccount?,
    val followsReady: Boolean,
    val loggingIn: Boolean,
    val loginPrompt: DeviceLoginPrompt?,
    val twitchLinkWarning: Boolean,
    val reloginForPermissions: Boolean,
    val reloginForSession: Boolean,
)

@Composable
internal fun collectAppAccount(accountViewModel: AccountViewModel): AppAccount {
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val followsReady by accountViewModel.followsReady.collectAsStateWithLifecycle()
    val loggingIn by accountViewModel.loggingIn.collectAsStateWithLifecycle()
    val loginPrompt by accountViewModel.loginPrompt.collectAsStateWithLifecycle()
    val twitchLinkWarning by accountViewModel.twitchLinkWarning.collectAsStateWithLifecycle()
    val reloginForPermissions by accountViewModel.reloginForPermissions.collectAsStateWithLifecycle()
    val reloginForSession by accountViewModel.reloginForSession.collectAsStateWithLifecycle()
    return AppAccount(
        account = account,
        followsReady = followsReady,
        loggingIn = loggingIn,
        loginPrompt = loginPrompt,
        twitchLinkWarning = twitchLinkWarning,
        reloginForPermissions = reloginForPermissions,
        reloginForSession = reloginForSession,
    )
}

/** The ids of the channels the user follows, once the follows are known. */
@Composable
internal fun rememberFollowedIds(homeViewModel: HomeViewModel, signedIn: Boolean): Set<String>? {
    val followedChannels by homeViewModel.follows.channels.collectAsStateWithLifecycle()
    val followedStatus by homeViewModel.follows.status.collectAsStateWithLifecycle()
    return remember(signedIn, followedStatus, followedChannels) {
        followedChannelIds(signedIn, followedStatus, followedChannels)
    }
}

/** Where the app's navigation goes: a bar at the bottom or a rail at the start, and the room it takes. */
internal data class AppLayout(
    val bottomBarShown: Boolean,
    val railShown: Boolean,
    val navigationRail: Boolean,
    /** The bar's height the players keep clear of; 0 with the rail. */
    val bottomBarHeightPx: Int,
    /** How far a mini player keeps from the bottom: clear of the bar when it shows. */
    val miniPlayerBottom: Dp,
    val screenWidthPx: Int,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun appLayout(shell: AppShellState, inPip: Boolean): AppLayout {
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeTargetBottomPx = WindowInsets.imeAnimationTarget.getBottom(density)
    val showNavigationBar = showAppNavigationBar(
        inPictureInPicture = inPip,
        imeBottomPx = imeBottomPx,
        imeTargetBottomPx = imeTargetBottomPx,
        keepVisibleWhileImeCloses = shell.keepNavigationBarWhileImeCloses,
        keyboardInHomeSearch = shell.homeSearchOpen,
    )
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx <= 0) shell.keepNavigationBarWhileImeCloses = false
    }
    val screenWidthPx = LocalWindowInfo.current.containerSize.width
    val navigationRail = appNavigationUsesRail(with(density) { screenWidthPx.toDp() }.value)
    // What the screens keep clear of: the bar's height at the bottom, or the rail's width at the start.
    val bottomBarShown = showNavigationBar && !navigationRail
    return AppLayout(
        bottomBarShown = bottomBarShown,
        railShown = showNavigationBar && navigationRail,
        navigationRail = navigationRail,
        bottomBarHeightPx = if (navigationRail) 0 else shell.navigationBarHeightPx,
        miniPlayerBottom = if (bottomBarShown) {
            with(density) { shell.navigationBarHeightPx.toDp() } + 12.dp
        } else {
            12.dp
        },
        screenWidthPx = screenWidthPx,
    )
}

/** What the app's parts read as of this composition, and what opens a stream, a recording, or the whispers. */
internal data class AppScene(
    val activity: MainActivity,
    val shell: AppShellState,
    val navController: NavHostController,
    val currentEntry: State<NavBackStackEntry?>,
    val homeViewModel: HomeViewModel,
    val accountViewModel: AccountViewModel,
    val ownChannelReads: OwnChannelReads,
    val pipResumePreferences: SharedPreferences,
    val accountState: AppAccount,
    val followedIds: Set<String>?,
    val inPip: Boolean,
    /** The stream screen is the current destination, as of this composition. */
    val watching: Boolean,
    val layout: AppLayout,
    val snackbarHostState: SnackbarHostState,
    val snackbarLift: SnackbarLift,
    val homeState: SaveableStateHolder,
) {
    val account: TwitchAccount? get() = accountState.account
    val recordingOpen: Boolean get() = shell.player.video != null

    /** A stream or a recording waits in the corner, where Back from what it opened can expand it again. */
    val playerMinimized: Boolean
        get() = (watching && shell.player.liveMinimized && !shell.player.liveClosedWithPip) ||
            (recordingOpen && shell.player.videoMinimized)

    /**
     * One player is on screen at a time: PlayerSession closes the recording when a stream opens,
     * and the open decision closes every stream in the back stack when a recording opens.
     */
    fun openProfileVideo(video: ProfileVideoPlayback, returnChannel: String? = null) {
        val decision = profileVideoOpenDecision(shell.section, shell.player.liveMinimized, watching)
        shell.player = shell.player.videoOpened(video, liveMinimized = decision.minimized, returnChannel)
        shell.section = decision.section
        if (decision.closeLivePlayer) navController.closeWatchedChannels()
    }

    /** A recording or a clip from a link closes the stream like any recording; Back on it opens that stream again. */
    fun openLinkedPlayback(video: ProfileVideoPlayback) {
        val channel = currentEntry.value?.arguments?.getString("channel")
        openProfileVideo(video, linkedVideoReturnChannel(watching, shell.player.liveMinimized, channel))
    }

    /**
     * Back from a recording or a clip opened over [channel]'s stream: it closes as the stream opens in its place,
     * without the rise from Home, as Back to the channel watched before does.
     */
    fun returnFromVideoToStream(channel: String) {
        AppLog.i(PLAYER_NAV_LOG_TAG, "back from the recording to $channel")
        openChannel(ChannelPreview(channel), fromHome = false)
    }

    /** Opens [channel]'s stream, which shows what the opener knows of the channel until its profile loads. */
    fun openChannel(channel: ChannelPreview, fromHome: Boolean) {
        AppLog.i(PLAYER_NAV_LOG_TAG, "open ${channel.login}, from home: $fromHome")
        shell.openPlayerFromHome = fromHome
        shell.player = shell.player.liveOpened()
        homeViewModel.addRecent(channel.login)
        val currentChannel = currentEntry.value?.arguments?.getString("channel")
        if (!(watching && currentChannel.equals(channel.login, ignoreCase = true))) {
            navController.navigate(watchRoute(channel)) {
                launchSingleTop = true
                popUpTo("home")
            }
        }
    }

    fun openWhispers() {
        if (shell.section != AppSection.Whispers) {
            shell.sectionBeforeWhispers = shell.section
            shell.expandPlayerAfterWhispers = shell.player.fullScreenBeforeWhispers(watching)
        }
        shell.player = shell.player.whispersOpened(watching)
        shell.section = AppSection.Whispers
    }

    /**
     * A login already running shows its loader again instead of doing nothing: the user may have left it for
     * another section while the browser took long, or after the page closed without telling the app.
     */
    fun logIn() {
        accountViewModel.logIn()
        if (accountViewModel.loggingIn.value) shell.section = AppSection.Account
    }

    suspend fun ownChatterFollow(broadcasterId: String): ChatterFollow? {
        val access = accountViewModel.session.userAccess() ?: return null
        return ownChannelReads.ownChatterFollow(access.accessToken, access.userId, broadcasterId)
    }

    suspend fun readOwnFollow(broadcasterId: String): FollowForChatMode {
        val access = accountViewModel.session.userAccess() ?: return FollowForChatMode.Unknown
        return when (val read = ownChannelReads.readOwnFollow(access.accessToken, access.userId, broadcasterId)) {
            is OwnFollowRead.Following -> FollowForChatMode.Following(read.atMillis)
            OwnFollowRead.NotFollowing -> FollowForChatMode.NotFollowing
            OwnFollowRead.Unavailable -> FollowForChatMode.Unknown
        }
    }

    suspend fun readOwnSubscription(broadcasterId: String): SubForChatMode {
        val access = accountViewModel.session.userAccess() ?: return SubForChatMode.Unknown
        return when (ownChannelReads.readOwnSubscription(access.accessToken, access.userId, broadcasterId)) {
            UserSubscriptionRead.Subscribed -> SubForChatMode.Subscribed
            UserSubscriptionRead.NotSubscribed -> SubForChatMode.NotSubscribed
            UserSubscriptionRead.Unavailable -> SubForChatMode.Unknown
        }
    }
}
