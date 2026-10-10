package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ChatPart
import name.alexwayfer.customtv.chat.findChatNicks

private object NoNickColors : State<Map<String, Color>> {
    override val value: Map<String, Color> = emptyMap()
}

/**
 * The colors of chatters seen so far, as a state that grows with the chat. A row reads only the
 * colors of the nicks in its own text, so a new chatter does not rebuild every visible row.
 */
internal val LocalChatNickColors = staticCompositionLocalOf<State<Map<String, Color>>> { NoNickColors }

/** The known colors of the nicks [message] shows, the only ones its text styling looks up. */
internal fun chatNickColorsFor(message: ChatMessage, nickColors: Map<String, Color>): Map<String, Color> {
    if (nickColors.isEmpty()) return emptyMap()
    val found = mutableMapOf<String, Color>()
    for (part in message.parts) {
        if (part !is ChatPart.Text) continue
        for ((_, _, _, key) in findChatNicks(part.text, nickColors.keys)) {
            nickColors[key]?.let { found[key] = it }
        }
    }
    return found
}
