package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import name.alexwayfer.customtv.R

@Composable
internal fun ActivityStatisticsSettings(viewModel: SettingsViewModel) {
    val enabled by viewModel.activityStatistics.enabled.collectAsStateWithLifecycle(initialValue = null)
    val restored = enabled ?: return
    SettingsSection(stringResource(R.string.settings_section_privacy))
    SettingsToggle(
        label = stringResource(R.string.activity_statistics),
        hint = stringResource(R.string.activity_statistics_hint),
        checked = restored,
        onCheckedChange = viewModel.activityStatistics::setEnabled,
    )
}
