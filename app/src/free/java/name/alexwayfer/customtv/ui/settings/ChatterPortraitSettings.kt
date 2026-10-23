package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/**
 * AI chatter portraits are a Premium feature: the free settings have no page for them, so this title is never shown.
 */
internal val ChatterPortraitSettingsTitle = R.string.settings

/** The AI portraits row shows locked, and a tap shows the Premium banner. */
@Composable
internal fun ChatterPortraitSettingsEntry(onOpen: () -> Unit) {
    SettingsLockedEntry(
        icon = painterResource(R.drawable.ic_auto_fix_high),
        title = stringResource(R.string.settings_section_chatter_portraits),
        source = "the AI portraits settings",
    )
}

@Composable
internal fun ChatterPortraitSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit
