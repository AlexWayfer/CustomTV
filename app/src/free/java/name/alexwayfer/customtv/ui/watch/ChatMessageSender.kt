package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import name.alexwayfer.customtv.chat.TwitchIrcSender
import name.alexwayfer.customtv.data.ChatSendResult
import name.alexwayfer.customtv.data.HTTP_UNAUTHORIZED

@Composable
internal fun rememberChatMessageSender(channel: ChatInputChannel, user: ChatInputUser): ChatMessageSender {
    val sender = remember(channel.login, user.login, user.userId) {
        user.login?.takeIf { !user.userId.isNullOrBlank() }?.let { TwitchIrcSender(channel.login, it) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(sender, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) sender?.disconnect()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            sender?.disconnect()
        }
    }
    val accessToken = user.accessToken
    return remember(sender, accessToken) {
        { message, replyParentId ->
            val token = accessToken()?.takeIf { it.isNotBlank() }
            if (sender == null || token == null) ChatSendResult.Rejected(HTTP_UNAUTHORIZED, "")
            else sender.send(token, message, replyParentId)
        }
    }
}
