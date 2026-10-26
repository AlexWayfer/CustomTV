package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
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
import name.alexwayfer.customtv.data.ThirdPartyChatOption

/** The chat page: what 7TV, FFZ, and BTTV add, how messages look, what sending does, and the messages Premium hides. */
@Composable
internal fun ChatSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    ThirdPartySettings(settings, viewModel)
    SettingsSection(stringResource(R.string.settings_section_messages))
    MeMessageStyle(settings, viewModel)
    SettingsToggle(
        label = stringResource(R.string.highlight_first_messages),
        hint = stringResource(R.string.highlight_first_messages_hint),
        checked = settings.highlightFirstMessages,
        onCheckedChange = viewModel.chat::setHighlightFirstMessages,
    )
    RaiderMarkSettings(settings, viewModel)
    MessageMenuModerationSettings(settings, viewModel)
    TopChatterBadgeSettings(settings, viewModel)
    LinkPreviewSettings(settings, viewModel)
    SettingsSection(stringResource(R.string.settings_section_sending))
    SettingsToggle(
        label = stringResource(R.string.emote_completion_without_colon),
        hint = stringResource(R.string.emote_completion_without_colon_hint),
        checked = settings.emoteCompletionWithoutColon,
        onCheckedChange = viewModel.chat::setEmoteCompletionWithoutColon,
    )
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

/** A section for each of 7TV, FFZ, and BTTV with the switches of what it adds to the chat. */
@Composable
private fun ThirdPartySettings(settings: AppSettings, viewModel: SettingsViewModel) {
    SettingsSection(stringResource(R.string.chat_emote_platform_7tv), first = true)
    ThirdPartyToggle(
        R.string.third_party_emotes, R.string.seven_tv_emotes_hint,
        settings.sevenTvEmotes, ThirdPartyChatOption.SevenTvEmotes, viewModel,
    )
    ThirdPartyToggle(
        R.string.third_party_badges, R.string.seven_tv_badges_hint,
        settings.sevenTvBadges, ThirdPartyChatOption.SevenTvBadges, viewModel,
    )
    ThirdPartyToggle(
        R.string.seven_tv_paints, R.string.seven_tv_paints_hint,
        settings.sevenTvPaints, ThirdPartyChatOption.SevenTvPaints, viewModel,
    )
    SettingsSection(stringResource(R.string.chat_emote_platform_ffz))
    ThirdPartyToggle(
        R.string.third_party_emotes, R.string.ffz_emotes_hint,
        settings.ffzEmotes, ThirdPartyChatOption.FfzEmotes, viewModel,
    )
    ThirdPartyToggle(
        R.string.third_party_badges, R.string.ffz_badges_hint,
        settings.ffzBadges, ThirdPartyChatOption.FfzBadges, viewModel,
    )
    SettingsSection(stringResource(R.string.chat_emote_platform_bttv))
    ThirdPartyToggle(
        R.string.third_party_emotes, R.string.bttv_emotes_hint,
        settings.bttvEmotes, ThirdPartyChatOption.BttvEmotes, viewModel,
    )
    ThirdPartyToggle(
        R.string.third_party_badges, R.string.bttv_badges_hint,
        settings.bttvBadges, ThirdPartyChatOption.BttvBadges, viewModel,
    )
}

/** A switch of what 7TV, FFZ, or BTTV adds; the hint names the service, as the label does not. */
@Composable
private fun ThirdPartyToggle(
    @StringRes label: Int,
    @StringRes hint: Int,
    checked: Boolean,
    option: ThirdPartyChatOption,
    viewModel: SettingsViewModel,
) {
    SettingsToggle(
        label = stringResource(label),
        hint = stringResource(hint),
        checked = checked,
        onCheckedChange = { viewModel.chat.setThirdPartyOption(option, it) },
    )
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
