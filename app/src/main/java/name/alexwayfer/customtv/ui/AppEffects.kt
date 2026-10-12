package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.auth.playerYieldsToReloginPrompt
import name.alexwayfer.customtv.auth.resumeSavedChannel
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.StreamAlertsLoginRequest
import name.alexwayfer.customtv.data.StreamStartAlertsController
import name.alexwayfer.customtv.data.StreamStartRequest
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.account.AccountNotice
import name.alexwayfer.customtv.ui.account.playerExpandsAfterLoginCancelled
import name.alexwayfer.customtv.ui.home.FollowedLoadFailedSnackbar
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.watch.SleepTimerEffect
import name.alexwayfer.customtv.ui.whispers.WhispersBackground
import name.alexwayfer.customtv.update.AppUpdateOpenRequest

/** What reaches the app from outside the screen: notifications, login requests, updates, and whispers. */
@Composable
internal fun AppRequestEffects(scene: AppScene) {
    val shell = scene.shell
    val accountState = scene.accountState
    LaunchedEffect(accountState.reloginForPermissions, accountState.reloginForSession) {
        if (playerYieldsToReloginPrompt(accountState.reloginForPermissions, accountState.reloginForSession)) {
            shell.player = shell.player.withLiveMinimized(true)
        }
    }
    val userId = scene.account?.userId
    LaunchedEffect(userId) {
        StreamStartAlertsController.onSignedIn(userId != null)
    }
    ExperimentalLoginAccount(userId)
    val openWhispersLatest by rememberUpdatedState(scene::openWhispers)
    WhispersBackground(userId = userId, onOpenRequested = { openWhispersLatest() })
    LaunchedEffect(Unit) {
        StreamStartRequest.login.collect { login ->
            val channel = login ?: return@collect
            StreamStartRequest.clear()
            scene.openChannel(channel, shell.section == AppSection.Home)
        }
    }
    LaunchedEffect(Unit) {
        // Starting the login also shows the profile, where it finishes.
        StreamAlertsLoginRequest.requested.collect { requested ->
            if (!requested) return@collect
            StreamAlertsLoginRequest.clear()
            scene.accountViewModel.logIn()
        }
    }
    val watchingLatest by rememberUpdatedState(scene.watching)
    LaunchedEffect(Unit) {
        // A full-screen player shrinks to the mini player, so the settings are in sight.
        AppUpdateOpenRequest.requested.collect { requested ->
            if (!requested) return@collect
            AppUpdateOpenRequest.clear()
            shell.player = shell.player.coveringPlayerMinimized(watchingLatest)
            shell.settingsScrollTarget = SettingsScrollTarget.Updates
            shell.section = AppSection.Settings
        }
    }
    val followsLoadFailed by scene.homeViewModel.follows.loadFailed.collectAsStateWithLifecycle()
    FollowedLoadFailedSnackbar(followsLoadFailed, scene.snackbarHostState) {
        scene.homeViewModel.follows.reload(scene.accountViewModel.session::userAccess)
    }
}

/** The stream screen opening and closing: the channel left in picture-in-picture, the sleep timer, and focus. */
@Composable
internal fun AppPlayerEffects(scene: AppScene) {
    val shell = scene.shell
    val inPip = scene.inPip
    SleepTimerEffect(scene.snackbarHostState, inPictureInPicture = inPip) {
        AppLog.i(PLAYER_NAV_LOG_TAG, "close the players by the sleep timer, in picture-in-picture: $inPip")
        shell.profileClosesUnderPlayer = false
        shell.player = shell.player.sleepTimerFired()
        scene.navController.popBackStack("home", inclusive = false)
        // Picture-in-picture would stay open on the home screen; it leaves with the task instead.
        if (inPip) scene.activity.moveTaskToBack(true)
    }
    ResumeChannelLeftInPip(scene)
    val watching = scene.watching
    LaunchedEffect(watching) {
        AppLog.i(PLAYER_NAV_LOG_TAG, if (watching) "stream screen shown" else "stream screen gone")
        // The reopened channel is on screen: the start has settled.
        if (watching) shell.startChannelPending = false
    }
    val rootView = LocalView.current
    val focusManager = LocalFocusManager.current
    DisposableEffect(rootView) {
        val wasFocusableInTouchMode = rootView.isFocusableInTouchMode
        rootView.isFocusableInTouchMode = true
        onDispose { rootView.isFocusableInTouchMode = wasFocusableInTouchMode }
    }
    LaunchedEffect(watching, shell.player.liveClosedWithPip) {
        if (!watching || shell.player.liveClosedWithPip) shell.channelLive = false
        if (watching) {
            shell.watchedThisSession = true
            return@LaunchedEffect
        }
        if (!shell.watchedThisSession) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        focusManager.clearFocus(force = true)
        rootView.clearFocus()
    }
}

/** At the start, reopens the channel the app was showing in picture-in-picture when it was closed. */
@Composable
private fun ResumeChannelLeftInPip(scene: AppScene) {
    val shell = scene.shell
    val entry = scene.currentEntry.value
    val relogin = scene.accountState.reloginForPermissions || scene.accountState.reloginForSession
    LaunchedEffect(entry) {
        entry ?: return@LaunchedEffect
        if (shell.pipResumeChecked) return@LaunchedEffect
        shell.pipResumeChecked = true
        val preferences = scene.pipResumePreferences
        val channelId = preferences.getString(PIP_RESUME_CHANNEL_ID, null)
        preferences.edit { remove(PIP_RESUME_CHANNEL_ID) }
        if (resumeSavedChannel(channelId, entry.destination.route == "home", relogin)) {
            val channel = channelId?.let { id -> ChannelAvatarRepository.refreshByIds(listOf(id)).firstOrNull()?.login }
            if (channel == null) {
                shell.startChannelPending = false
                return@LaunchedEffect
            }
            AppLog.i(PLAYER_NAV_LOG_TAG, "reopen $channel left in picture-in-picture")
            shell.player = shell.player.liveOpened()
            scene.navController.navigate("watch/$channel") {
                launchSingleTop = true
                popUpTo("home")
            }
        } else {
            shell.startChannelPending = false
        }
    }
}

/** Picture-in-picture: allowed while a live stream or a recording plays, and what leaving or closing it does. */
@Composable
internal fun AppPipEffects(scene: AppScene) {
    val shell = scene.shell
    val activity = scene.activity
    val inPip = scene.inPip
    val watching = scene.watching
    LaunchedEffect(inPip, watching) {
        if (shell.wasInPip && !inPip) {
            shell.player = shell.player.pipExpanded(watching)
        }
        shell.wasInPip = inPip
    }
    // Closing picture-in-picture closes the recording in it. The stream binds its own handler.
    val recordingOpen = scene.recordingOpen
    DisposableEffect(activity, recordingOpen) {
        if (!recordingOpen) return@DisposableEffect onDispose { }
        val closeRecording: () -> Unit = { shell.player = shell.player.videoClosed() }
        activity.onPipClosed = closeRecording
        onDispose {
            if (activity.onPipClosed === closeRecording) activity.onPipClosed = null
        }
    }
    PremiumBanner(
        playerOpen = watching || recordingOpen,
        inPictureInPicture = inPip,
        onHome = shell.section == AppSection.Home,
        startSettled = !shell.startChannelPending,
    )
    val pipEnabled = pictureInPictureAllowed(
        watching = watching,
        channelLive = shell.channelLive,
        liveClosedWithPip = shell.player.liveClosedWithPip,
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
}

/** The login: its notices as snackbars, and where the app goes once it finishes or is cancelled. */
@Composable
internal fun AppLoginEffects(scene: AppScene) {
    val shell = scene.shell
    val accountViewModel = scene.accountViewModel
    val accountNotice by accountViewModel.notice.collectAsStateWithLifecycle()
    val loginFailedMessage = stringResource(R.string.twitch_login_failed)
    val loginTimedOutMessage = stringResource(R.string.twitch_login_timed_out)
    val clientMissingMessage = stringResource(R.string.twitch_client_missing)
    val linksStillMessage = stringResource(R.string.twitch_links_still)
    LaunchedEffect(accountNotice) {
        val notice = accountNotice ?: return@LaunchedEffect
        val message = when (notice) {
            AccountNotice.Failed -> loginFailedMessage
            AccountNotice.TimedOut -> loginTimedOutMessage
            AccountNotice.MissingClient -> clientMissingMessage
            AccountNotice.LinksStillClaimed -> linksStillMessage
        }
        accountViewModel.consumeNotice()
        scene.snackbarHostState.showSnackbar(message)
    }
    val loggingIn = scene.accountState.loggingIn
    LaunchedEffect(loggingIn) {
        if (loggingIn) {
            shell.awaitingLogin = true
            shell.section = AppSection.Account
        }
    }
    val account = scene.account
    val twitchLinkWarning = scene.accountState.twitchLinkWarning
    LaunchedEffect(account, shell.awaitingLogin, loggingIn, twitchLinkWarning) {
        if (shell.awaitingLogin && account != null) {
            shell.awaitingLogin = false
            shell.returnToStreamAfterLogin = false
            shell.section = AppSection.Account
        } else if (shell.awaitingLogin && !loggingIn && !twitchLinkWarning) {
            shell.awaitingLogin = false
            val streamOpen = scene.watching && !shell.player.liveClosedWithPip
            if (playerExpandsAfterLoginCancelled(shell.returnToStreamAfterLogin, streamOpen)) {
                shell.player = shell.player.withLiveMinimized(false)
            }
            shell.returnToStreamAfterLogin = false
            shell.section = AppSection.Home
        }
    }
}
