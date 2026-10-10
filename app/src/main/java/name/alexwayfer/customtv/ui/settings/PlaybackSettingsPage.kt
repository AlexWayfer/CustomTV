package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** The playback page. */
@Composable
internal fun PlaybackSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsToggle(
        label = stringResource(R.string.background_sound_only),
        hint = stringResource(R.string.background_sound_only_hint),
        checked = settings.backgroundSoundOnly,
        onCheckedChange = viewModel::setBackgroundSoundOnly,
    )
}
