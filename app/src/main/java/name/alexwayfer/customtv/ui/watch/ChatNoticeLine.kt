package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatNotice
import name.alexwayfer.customtv.ui.theme.PremiumGold
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import androidx.compose.material3.MaterialTheme

@Composable
internal fun ChatNoticeLine(
    notice: ChatNotice,
    textSize: TextUnit,
    lineHeight: TextUnit,
) {
    val text = when (notice) {
        ChatNotice.Connecting -> stringResource(R.string.chat_connecting)
        ChatNotice.Welcome -> stringResource(R.string.chat_welcome)
        ChatNotice.Disconnected -> stringResource(R.string.chat_disconnected)
        ChatNotice.Reconnecting -> stringResource(R.string.chat_reconnecting)
        ChatNotice.Connected -> stringResource(R.string.chat_connected)
        ChatNotice.RecentChatFailed -> stringResource(R.string.recent_chat_load_failed)
        ChatNotice.ChannelNotFound -> stringResource(R.string.channel_not_found)
    }
    Text(
        text = text,
        color = when (notice) {
            ChatNotice.RecentChatFailed -> MaterialTheme.colorScheme.error
            ChatNotice.ChannelNotFound -> PremiumGold
            else -> TwitchTextSecondary
        },
        fontSize = textSize,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        lineHeight = lineHeight,
    )
}
