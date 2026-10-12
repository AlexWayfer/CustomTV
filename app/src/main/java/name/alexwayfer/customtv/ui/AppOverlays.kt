package name.alexwayfer.customtv.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import name.alexwayfer.customtv.ui.account.TwitchLinkWarningDialog
import name.alexwayfer.customtv.ui.account.TwitchPermissionsDialog
import name.alexwayfer.customtv.ui.account.TwitchSessionEndedDialog
import name.alexwayfer.customtv.ui.account.openTwitchLinkSettings
import name.alexwayfer.customtv.ui.account.openTwitchLoginPage
import name.alexwayfer.customtv.ui.account.releaseLoginBrowser

/** The Twitch login page while a login waits for it, and the dialogs about links, permissions, and the session. */
@Composable
internal fun AppLoginPrompts(scene: AppScene) {
    val activity = scene.activity
    val accountViewModel = scene.accountViewModel
    val prompt = scene.accountState.loginPrompt
    val loginSheetScreenPx = LocalWindowInfo.current.containerSize.height
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (prompt != null) {
        DisposableEffect(prompt.verificationUri) {
            onDispose { releaseLoginBrowser(activity) }
        }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) accountViewModel.deviceLogin.reopen.appResumed()
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        val openLoginPage = {
            openTwitchLoginPage(
                activity,
                prompt.verificationUri,
                loginSheetScreenPx,
                onHidden = accountViewModel.deviceLogin::pageClosed,
                onLaunched = accountViewModel.deviceLogin.reopen::pageLaunched,
            )
        }
        LaunchedEffect(prompt.verificationUri) { openLoginPage() }
        val loginPageReopen = scene.shell.loginPageReopen
        LaunchedEffect(loginPageReopen) {
            loginPageReopen?.take() ?: return@LaunchedEffect
            openLoginPage()
        }
    }
    if (scene.accountState.twitchLinkWarning) {
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) accountViewModel.deviceLogin.refreshTwitchLinks()
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        TwitchLinkWarningDialog(
            onOpenSettings = { openTwitchLinkSettings(activity) },
            onDismiss = { accountViewModel.deviceLogin.dismissTwitchLinkWarning() },
        )
    }
    if (scene.accountState.reloginForPermissions) {
        TwitchPermissionsDialog(
            onLogIn = { accountViewModel.confirmScopeRelogin() },
            onDismiss = { accountViewModel.relogin.dismissPermissions() },
        )
    } else if (scene.accountState.reloginForSession) {
        TwitchSessionEndedDialog(
            onLogIn = { accountViewModel.confirmSessionRelogin() },
            onDismiss = { accountViewModel.relogin.dismissSession() },
        )
    }
}

@Composable
internal fun BoxScope.AppSnackbarHost(scene: AppScene) {
    val density = LocalDensity.current
    val shell = scene.shell
    val layout = scene.layout
    val snackbarLift = scene.snackbarLift
    SnackbarHost(
        hostState = scene.snackbarHostState,
        snackbar = { AppSnackbar(it) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(
                // The full screen player covers the rail.
                start = if (layout.railShown && !snackbarLift.overPlayer) {
                    with(density) { shell.navigationRailWidthPx.toDp() }
                } else {
                    0.dp
                },
                bottom = if (layout.bottomBarShown) with(density) { shell.navigationBarHeightPx.toDp() } else 0.dp,
            )
            // Without the app's bar, as over the player, it keeps clear of the system's.
            .then(if (layout.bottomBarShown) Modifier else Modifier.navigationBarsPadding())
            .then(if (snackbarLift.overPlayer) Modifier.widthIn(max = SnackbarOverPlayerMaxWidth) else Modifier)
            .offset { IntOffset(0, -snackbarLift.px()) }
            // Above the players, so a tip or a notice shows over the stream too.
            .zIndex(SNACKBAR_Z_INDEX),
    )
}

private const val SNACKBAR_Z_INDEX = 3f
