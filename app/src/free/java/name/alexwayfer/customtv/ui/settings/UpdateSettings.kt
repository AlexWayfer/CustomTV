package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.update.AppUpdates
import name.alexwayfer.customtv.update.GITHUB_RELEASES_PAGE_URL

/** The free build's updates come from its public GitHub releases, so the section needs no account. */
@Composable
internal fun UpdateSettings() {
    SettingsSection(stringResource(R.string.settings_section_updates))
    val status by AppUpdates.status.collectAsStateWithLifecycle()
    val download by AppUpdates.download.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AppUpdateSection(
        status = status,
        download = download,
        onCheck = AppUpdates::checkAgain,
        onDownload = AppUpdates::download,
    ) {
        SmallButton(
            text = stringResource(R.string.app_update_open_group),
            onClick = { openLink(context, FREE_RELEASES_TELEGRAM_URL) },
            colors = secondaryButtonColors(),
        )
        SmallButton(
            text = stringResource(R.string.github_open),
            onClick = { openLink(context, GITHUB_RELEASES_PAGE_URL) },
            colors = secondaryButtonColors(),
        )
    }
}
