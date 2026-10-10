package name.alexwayfer.customtv.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.telegram.TelegramSession
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.update.APP_UPDATE_TAG
import name.alexwayfer.customtv.update.AppInstallStep
import name.alexwayfer.customtv.update.AppRelease
import name.alexwayfer.customtv.update.AppUpdateAction
import name.alexwayfer.customtv.update.AppUpdateDownload
import name.alexwayfer.customtv.update.AppUpdateInstaller
import name.alexwayfer.customtv.update.appInstallStep
import name.alexwayfer.customtv.update.appUpdateAction
import name.alexwayfer.customtv.update.showsHintAfterSettings

/** A newer release: download it from Telegram with progress, then install it. */
@Composable
internal fun AppUpdateBlock(release: AppRelease, download: AppUpdateDownload) {
    val version = release.version.toString()
    Column {
        GroupStatus(stringResource(R.string.app_update_available, version))
        // Download fades into progress, and progress into Install.
        AnimatedContent(
            targetState = appUpdateAction(release, download),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it.javaClass },
            label = "appUpdateAction",
        ) { action ->
            when (action) {
                is AppUpdateAction.Download -> Column {
                    SmallButton(
                        text = stringResource(R.string.app_update_download, version),
                        onClick = { TelegramSession.downloadUpdate(release.fileId) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    UpdateError(action.error)
                }
                is AppUpdateAction.Downloading -> DownloadProgress(action.fraction)
                is AppUpdateAction.Install -> InstallButton(version, action.path)
            }
        }
    }
}

@Composable
private fun DownloadProgress(target: Float) {
    val fraction by animateFloatAsState(target, label = "appUpdateDownload")
    Column(modifier = Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) {}) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = TwitchPurple,
            trackColor = TwitchDivider,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        GroupStatus(stringResource(R.string.app_update_downloading, (target * 100).roundToInt()))
    }
}

@Composable
private fun InstallButton(version: String, path: String) {
    val context = LocalContext.current
    val busy by AppUpdateInstaller.busy.collectAsStateWithLifecycle()
    val error by AppUpdateInstaller.error.collectAsStateWithLifecycle()
    var hintShown by rememberSaveable { mutableStateOf(false) }
    val allowInstalls = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val allowed = AppUpdateInstaller.canInstall(context)
        AppLog.i(APP_UPDATE_TAG, if (allowed) "installs allowed" else "installs still not allowed")
        hintShown = showsHintAfterSettings(allowed)
        if (hintShown) AppLog.i(APP_UPDATE_TAG, "Play Protect hint shown")
    }
    Column {
        SmallButton(
            text = stringResource(R.string.app_update_install, version),
            onClick = {
                when (appInstallStep(AppUpdateInstaller.canInstall(context))) {
                    AppInstallStep.AllowInstalls -> {
                        AppLog.i(APP_UPDATE_TAG, "asking to allow installs")
                        allowInstalls.launch(AppUpdateInstaller.installPermissionIntent(context))
                    }
                    AppInstallStep.PlayProtectHint -> {
                        AppLog.i(APP_UPDATE_TAG, "Play Protect hint shown")
                        hintShown = true
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = !busy,
        )
        UpdateError(error)
    }
    if (hintShown) {
        PlayProtectHintDialog(
            onInstall = {
                hintShown = false
                AppUpdateInstaller.install(context, path, version)
            },
            onDismiss = {
                AppLog.i(APP_UPDATE_TAG, "Play Protect hint dismissed")
                hintShown = false
            },
        )
    }
}

@Composable
private fun UpdateError(error: UiText?) {
    val shown = rememberLastNonNull(error)
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = shown?.asString().orEmpty(),
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
