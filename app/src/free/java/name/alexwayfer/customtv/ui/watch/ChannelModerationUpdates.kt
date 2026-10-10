package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.data.ChannelEventSubClient

/** Moderator tools are Premium; the free build subscribes to nothing moderators see. */
@Suppress("unused")
@Composable
internal fun ChannelModerationUpdates(
    client: ChannelEventSubClient,
    broadcasterId: String?,
    moderatorId: String?,
    accessToken: suspend () -> String?,
    chatViewModel: ChatViewModel,
) = Unit
