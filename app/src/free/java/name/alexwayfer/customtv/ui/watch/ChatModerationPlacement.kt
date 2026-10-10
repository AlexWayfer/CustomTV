package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import name.alexwayfer.customtv.chat.ChatMessage

/** Moderator tools are Premium: the free build shows every message without a moderator frame. */
@Suppress("unused")
@Composable
internal fun ChatModerationFrame(
    message: ChatMessage,
    iconSize: Dp,
    textSize: TextUnit,
    content: @Composable () -> Unit,
) {
    content()
}
