package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** Vibration and sound on mentions and the recent chat, then the mention settings only Premium has. */
@Composable
internal fun MentionAlertSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsToggle(
        label = stringResource(R.string.mention_vibration),
        hint = stringResource(R.string.mention_vibration_hint),
        checked = settings.mentionVibration,
        onCheckedChange = viewModel::setMentionVibration,
    )
    SettingDetails(visible = settings.mentionVibration) {
        MentionVibrationControls(
            durationMs = settings.mentionVibrationMs,
            strengthPercent = settings.mentionVibrationPercent,
            onDurationChange = viewModel::setMentionVibrationMs,
            onStrengthChange = viewModel::setMentionVibrationPercent,
        )
    }
    SettingsToggle(
        label = stringResource(R.string.mention_sound),
        hint = stringResource(R.string.mention_sound_hint),
        checked = settings.mentionSound,
        onCheckedChange = viewModel::setMentionSound,
    )
    SettingDetails(visible = settings.mentionSound) {
        MentionSoundChoice(
            storedUri = settings.mentionSoundUri,
            onUriChange = viewModel::setMentionSoundUri,
        )
    }
    RecentChatSettings(
        enabled = settings.loadRecentChatOnOpen,
        onEnabledChange = viewModel::setLoadRecentChatOnOpen,
    )
    PremiumMentionSettings(settings, viewModel)
}
