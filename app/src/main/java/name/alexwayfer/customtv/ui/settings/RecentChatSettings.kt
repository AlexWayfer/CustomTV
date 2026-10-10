package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

@Composable
internal fun RecentChatSettings(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    SettingsToggle(
        label = stringResource(R.string.load_recent_chat_on_open),
        hint = stringResource(R.string.load_recent_chat_on_open_hint),
        checked = enabled,
        onCheckedChange = onEnabledChange,
    )
}
