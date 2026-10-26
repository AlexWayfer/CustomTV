package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.settings.SettingsScreen
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.watch.OneShotRequest
import name.alexwayfer.customtv.ui.watch.PlayerSheetReturn

/** The settings, which keep their state while a profile opened from them is open, so Back returns to them as left. */
@Composable
internal fun SettingsSectionPage(scene: AppScene) {
    val shell = scene.shell
    val scope = rememberCoroutineScope()
    val visit = shell.settingsVisit
    // The last visit's state is not coming back.
    LaunchedEffect(visit) { scene.homeState.removeState(settingsStateKey(visit - 1)) }
    scene.homeState.SaveableStateProvider(settingsStateKey(visit)) {
        SettingsScreen(
            onClose = { shell.section = AppSection.Home },
            onOpenCategory = scene::openExploreCategoryFromSettings,
            onOpenProfile = scene::openProfileFromSettings,
            onNotice = { message ->
                scope.launch {
                    // A tap again replaces the notice instead of queueing another behind it.
                    scene.snackbarHostState.currentSnackbarData?.dismiss()
                    scene.snackbarHostState.showSnackbar(message)
                }
            },
            signedIn = scene.account != null,
            accountName = scene.account?.displayName,
            reselected = shell.settingsReselected,
            scrollTo = shell.settingsScrollTarget,
            onScrolled = { shell.settingsScrollTarget = null },
            onBackToPlayer = shell.settingsPlayerReturn?.takeIf { scene.watching }?.let { sheet ->
                { scene.returnToPlayerSheet(sheet) }
            },
            onPlayerReturnDropped = { shell.settingsPlayerReturn = null },
        )
    }
    // Left any other way, such as through the navigation bar, the settings no longer lead back to the sheet.
    DisposableEffect(Unit) {
        onDispose { shell.settingsPlayerReturn = null }
    }
}

/** Opens the settings at [target] as the stream minimizes; Back from them reopens [sheetReturn] when there is one. */
internal fun AppScene.openSettingsFromPlayer(target: SettingsScrollTarget, sheetReturn: PlayerSheetReturn?) {
    shell.settingsScrollTarget = target
    shell.settingsPlayerReturn = sheetReturn
    shell.section = AppSection.Settings
}

/**
 * Back from the settings a stream's sheet opened expands the stream, which reopens that sheet once it shows its
 * content; the settings close under the covering stream, as a profile opened from it does.
 */
private fun AppScene.returnToPlayerSheet(sheet: PlayerSheetReturn) {
    shell.settingsPlayerReturn = null
    shell.playerSheetReopen = OneShotRequest(sheet)
    shell.player = shell.player.withLiveMinimized(false)
}

/** Opens [channel]'s profile over the settings, which Back from it returns to. */
private fun AppScene.openProfileFromSettings(channel: ChannelPreview) {
    shell.profileChannel = channel
    shell.profileOpenedFrom = AppSection.Settings
    shell.section = AppSection.ChannelProfile
}
