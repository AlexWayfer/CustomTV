package name.alexwayfer.customtv.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.data.OwnChannelReads
import name.alexwayfer.customtv.ui.account.AccountViewModel
import name.alexwayfer.customtv.ui.home.HomeViewModel
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.watch.ClearFocusWhenImeDismissed
import name.alexwayfer.customtv.ui.watch.ImeDismissFocus
import name.alexwayfer.customtv.ui.watch.LocalImeDismissFocus
import name.alexwayfer.customtv.ui.watch.ModerationFailureSnackbar
import name.alexwayfer.customtv.ui.watch.WATCH_CHANNEL_ROUTE

@Composable
fun CustomTvApp() {
    val activity = LocalActivity.current as MainActivity
    val inPip by activity.inPictureInPicture.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val currentEntry = navController.currentBackStackEntryAsState()
    val shell = rememberAppShellState()
    val accountState = collectAppAccount(accountViewModel)
    val scene = AppScene(
        activity = activity,
        shell = shell,
        navController = navController,
        currentEntry = currentEntry,
        homeViewModel = homeViewModel,
        accountViewModel = accountViewModel,
        ownChannelReads = remember { OwnChannelReads(BuildConfig.TWITCH_CLIENT_ID) },
        pipResumePreferences = remember(activity) { activity.getSharedPreferences(PIP_RESUME_PREFERENCES, 0) },
        accountState = accountState,
        followedIds = rememberFollowedIds(homeViewModel, signedIn = accountState.account != null),
        inPip = inPip,
        watching = currentEntry.value?.destination?.route == WATCH_CHANNEL_ROUTE,
        layout = appLayout(shell, inPip),
        snackbarHostState = remember { SnackbarHostState() },
        snackbarLift = remember { SnackbarLift() },
        homeState = rememberSaveableStateHolder(),
    )
    AppRequestEffects(scene)
    AppPlayerEffects(scene)
    AppPipEffects(scene)
    AppLoginEffects(scene)
    val imeDismissFocus = remember { ImeDismissFocus() }
    val gestureHints = rememberGestureHintReporter()
    gestureHints.Snackbars(scene.snackbarHostState)
    CopiedSnackbar(scene.snackbarHostState)
    ModerationFailureSnackbar(scene.snackbarHostState)
    CompositionLocalProvider(
        LocalImeDismissFocus provides imeDismissFocus,
        LocalUserAccess provides accountViewModel.session::userAccess,
        LocalGestureHints provides gestureHints,
        LocalSnackbarLift provides scene.snackbarLift,
    ) {
        ClearFocusWhenImeDismissed()
        AppScreens(scene)
    }
}

/** Home and its bar, the players over it, the sections, the login dialogs, and the snackbars over everything. */
@Composable
private fun AppScreens(scene: AppScene) {
    val shell = scene.shell
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TwitchBg),
    ) {
        AppHomeLayer(scene)
        if (scene.watching && !shell.player.liveMinimized && !scene.inPip) {
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
        AppPlayerHost(scene)
        AppMiniPlayerBack(scene)
        val sectionKeptInPip = shell.player.sectionKeptInPip(scene.inPip, scene.watching, shell.section)
        if (showAppShell(scene.inPip, scene.watching, shell.player.liveMinimized, shell.section) || sectionKeptInPip) {
            AppSections(scene, hidden = sectionKeptInPip)
        }
        AppRecordingPlayer(scene)
        AppLoginPrompts(scene)
        AppSnackbarHost(scene)
    }
}

private const val PIP_RESUME_PREFERENCES = "pip_resume"
internal const val PIP_RESUME_CHANNEL_ID = "channel"
internal const val PLAYER_NAV_LOG_TAG = "PlayerNav"
