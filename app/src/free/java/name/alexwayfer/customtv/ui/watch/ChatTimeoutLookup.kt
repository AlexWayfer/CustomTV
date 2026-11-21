package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import name.alexwayfer.customtv.chat.ChatTimeoutCheck

@Composable
internal fun rememberChatTimeoutLookup(channelId: String?, userId: String?): (suspend () -> ChatTimeoutCheck?)? =
    remember(channelId, userId) { null }
