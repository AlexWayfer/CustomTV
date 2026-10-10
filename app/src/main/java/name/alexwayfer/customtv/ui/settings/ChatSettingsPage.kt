package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** The chat page: emotes and how messages look. */
@Composable
internal fun ChatSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsSection(stringResource(R.string.settings_section_emotes), first = true)
    SettingsToggle(
        label = stringResource(R.string.seven_tv_emotes),
        hint = stringResource(R.string.seven_tv_emotes_hint),
        checked = settings.sevenTvEmotes,
        onCheckedChange = viewModel::setSevenTvEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.ffz_emotes),
        hint = stringResource(R.string.ffz_emotes_hint),
        checked = settings.ffzEmotes,
        onCheckedChange = viewModel::setFfzEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.bttv_emotes),
        hint = stringResource(R.string.bttv_emotes_hint),
        checked = settings.bttvEmotes,
        onCheckedChange = viewModel::setBttvEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.emote_completion_without_colon),
        hint = stringResource(R.string.emote_completion_without_colon_hint),
        checked = settings.emoteCompletionWithoutColon,
        onCheckedChange = viewModel::setEmoteCompletionWithoutColon,
    )
    SettingsSection(stringResource(R.string.settings_section_messages))
    Text(
        text = stringResource(R.string.me_messages),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        SegmentedButton(
            selected = settings.meMessageItalic,
            onClick = { viewModel.setMeMessageItalic(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(
                text = stringResource(R.string.me_messages_italic),
                fontStyle = FontStyle.Italic,
            )
        }
        SegmentedButton(
            selected = !settings.meMessageItalic,
            onClick = { viewModel.setMeMessageItalic(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(R.string.me_messages_name_color))
        }
    }
    Text(
        text = stringResource(R.string.me_messages_hint),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    SettingsToggle(
        label = stringResource(R.string.highlight_first_messages),
        hint = stringResource(R.string.highlight_first_messages_hint),
        checked = settings.highlightFirstMessages,
        onCheckedChange = viewModel::setHighlightFirstMessages,
    )
    RaiderMarkSettings(settings, viewModel)
    LinkPreviewSettings(settings, viewModel)
    SettingsToggle(
        label = stringResource(R.string.keep_keyboard_after_send),
        hint = stringResource(R.string.keep_keyboard_after_send_hint),
        checked = settings.keepKeyboardAfterSend,
        onCheckedChange = viewModel::setKeepKeyboardAfterSend,
    )
}
