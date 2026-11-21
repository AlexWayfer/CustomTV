package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.player.rememberProximityScreenOffSupported

/** The playback page. */
@Composable
internal fun PlaybackSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsToggle(
        label = stringResource(R.string.smooth_sound),
        hint = stringResource(R.string.smooth_sound_hint),
        checked = settings.smoothSound,
        onCheckedChange = viewModel::setSmoothSound,
    )
    if (rememberProximityScreenOffSupported()) {
        SettingsToggle(
            label = stringResource(R.string.blank_screen_on_proximity),
            hint = stringResource(R.string.blank_screen_on_proximity_hint),
            checked = settings.blankScreenOnProximity,
            onCheckedChange = viewModel::setBlankScreenOnProximity,
        )
    }
}
