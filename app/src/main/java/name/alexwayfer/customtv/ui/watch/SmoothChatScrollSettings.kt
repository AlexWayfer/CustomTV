package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

@Composable
internal fun SmoothChatScrollSettings(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ChatSettingsToggle(
        label = stringResource(R.string.smooth_new_messages),
        checked = enabled,
        onCheckedChange = onChange,
    )
    Text(
        text = stringResource(R.string.smooth_new_messages_hint),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
