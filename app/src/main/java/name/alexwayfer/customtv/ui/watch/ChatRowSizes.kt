package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/** The sizes a chat row draws its text, emotes, GIFs, and badges with at the chosen [chatTextSize]. */
internal data class ChatRowSizes(
    val chatTextSize: Int,
    val text: TextUnit,
    val timestamp: TextUnit,
    val lineHeight: TextUnit,
    val emote: Dp,
    val gif: Dp,
    val badge: Dp,
)

internal fun chatRowSizes(chatTextSize: Int): ChatRowSizes = ChatRowSizes(
    chatTextSize = chatTextSize,
    text = chatSp(chatTextSize, normal = 13, large = 16),
    timestamp = chatSp(chatTextSize, normal = 12, large = 14),
    lineHeight = chatSp(chatTextSize, normal = 21, large = 25),
    emote = chatDp(chatTextSize, normal = 22, large = 26),
    gif = chatDp(chatTextSize, normal = 96, large = 112),
    badge = chatDp(chatTextSize, normal = 16, large = 20),
)
