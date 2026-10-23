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
import name.alexwayfer.customtv.data.ChatSendOption

/** The chat page: emotes, how messages look, what sending does, and the messages Premium hides. */
@Composable
internal fun ChatSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsSection(stringResource(R.string.settings_section_emotes), first = true)
    SettingsToggle(
        label = stringResource(R.string.seven_tv_emotes),
        hint = stringResource(R.string.seven_tv_emotes_hint),
        checked = settings.sevenTvEmotes,
        onCheckedChange = viewModel.chat::setSevenTvEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.ffz_emotes),
        hint = stringResource(R.string.ffz_emotes_hint),
        checked = settings.ffzEmotes,
        onCheckedChange = viewModel.chat::setFfzEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.bttv_emotes),
        hint = stringResource(R.string.bttv_emotes_hint),
        checked = settings.bttvEmotes,
        onCheckedChange = viewModel.chat::setBttvEmotes,
    )
    SettingsToggle(
        label = stringResource(R.string.emote_completion_without_colon),
        hint = stringResource(R.string.emote_completion_without_colon_hint),
        checked = settings.emoteCompletionWithoutColon,
        onCheckedChange = viewModel.chat::setEmoteCompletionWithoutColon,
    )
    SettingsSection(stringResource(R.string.settings_section_messages))
    MeMessageStyle(settings, viewModel)
    SettingsToggle(
        label = stringResource(R.string.highlight_first_messages),
        hint = stringResource(R.string.highlight_first_messages_hint),
        checked = settings.highlightFirstMessages,
        onCheckedChange = viewModel.chat::setHighlightFirstMessages,
    )
    RaiderMarkSettings(settings, viewModel)
    TopChatterBadgeSettings(settings, viewModel)
    LinkPreviewSettings(settings, viewModel)
    SettingsSection(stringResource(R.string.settings_section_sending))
    SettingsToggle(
        label = stringResource(R.string.keep_keyboard_after_send),
        hint = stringResource(R.string.keep_keyboard_after_send_hint),
        checked = settings.keepKeyboardAfterSend,
        onCheckedChange = { viewModel.chat.setSendOption(ChatSendOption.KeepKeyboard, it) },
    )
    SettingsToggle(
        label = stringResource(R.string.scroll_to_latest_after_send),
        hint = stringResource(R.string.scroll_to_latest_after_send_hint),
        checked = settings.scrollToLatestAfterSend,
        onCheckedChange = { viewModel.chat.setSendOption(ChatSendOption.ScrollToLatest, it) },
    )
    SettingsToggle(
        label = stringResource(R.string.sent_message_history),
        hint = stringResource(R.string.sent_message_history_hint),
        checked = settings.sentMessageHistory,
        onCheckedChange = { viewModel.chat.setSendOption(ChatSendOption.SentMessageHistory, it) },
    )
    HiddenMessageSettings(settings, viewModel)
}

/** How a `/me` message looks: in italics, or in the chatter's name color. */
@Composable
private fun MeMessageStyle(settings: AppSettings, viewModel: SettingsViewModel) {
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
            onClick = { viewModel.chat.setMeMessageItalic(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(
                text = stringResource(R.string.me_messages_italic),
                fontStyle = FontStyle.Italic,
            )
        }
        SegmentedButton(
            selected = !settings.meMessageItalic,
            onClick = { viewModel.chat.setMeMessageItalic(false) },
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
}
