package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** The mentions page: how a mention stands out in chat and alerts. */
@Composable
internal fun MentionSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsToggle(
        label = stringResource(R.string.highlight_mentions),
        hint = stringResource(R.string.highlight_mentions_hint),
        checked = settings.highlightMentions,
        onCheckedChange = viewModel::setHighlightMentions,
    )
    MentionAlertSettings(settings, viewModel)
}
