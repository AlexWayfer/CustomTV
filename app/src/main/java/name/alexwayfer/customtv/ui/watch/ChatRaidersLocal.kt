package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.compositionLocalOf
import name.alexwayfer.customtv.chat.ChatRaider

/** Messages of chatters who came with a raid, by Twitch message ID, for the rows of the chat pane. */
internal val LocalChatRaiders = compositionLocalOf<Map<String, ChatRaider>> { emptyMap() }
