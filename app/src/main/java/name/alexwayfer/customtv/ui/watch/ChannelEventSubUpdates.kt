package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.chat.ChatViewModel
import name.alexwayfer.customtv.chat.ownMessageHoldRows
import name.alexwayfer.customtv.chat.withChatRowOnce
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelEventSubClient

/** One EventSub socket per open live channel: stream status, the user's own held messages, and what moderators see. */
@Composable
internal fun ChannelEventSubUpdates(
    broadcasterId: String?,
    signedIn: Boolean,
    signedInUserId: String?,
    accessToken: suspend () -> String?,
    chatViewModel: ChatViewModel,
) {
    val client = remember { ChannelEventSubClient(BuildConfig.TWITCH_CLIENT_ID) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    LaunchedEffect(broadcasterId, signedIn, signedInUserId) {
        val id = broadcasterId?.takeIf { it.isNotBlank() }
        val token = if (signedIn && id != null) accessToken() else null
        if (id == null || token == null) {
            client.disconnect()
        } else {
            client.listen(id, token, signedInUserId)
        }
    }
    LaunchedEffect(client) {
        client.events.collect { event ->
            ChannelAvatarRepository.applyStreamStatus(event)
        }
    }
    LaunchedEffect(client, chatViewModel) {
        client.userNotifications.collect { raw ->
            val rows = ownMessageHoldRows(raw, chatViewModel.nickColors.value, System.currentTimeMillis())
            if (rows.isNotEmpty()) {
                chatViewModel.editMessages { messages -> rows.fold(messages, ::withChatRowOnce) }
            }
        }
    }
    ChannelModerationUpdates(
        client = client,
        broadcasterId = broadcasterId,
        moderatorId = signedInUserId,
        accessToken = accessToken,
        chatViewModel = chatViewModel,
    )
}
