package name.alexwayfer.customtv.ui.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/**
 * The last profile tab. It is selected only on the way to it: resting on it opens the channel's stream with chat,
 * and the profile goes back to its first tab.
 */
@Composable
internal fun ChannelChatTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = { Text(stringResource(R.string.profile_chat)) },
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
