package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatModeChange
import name.alexwayfer.customtv.data.ChatSendError

/**
 * What the streamer or a moderator of the open channel can do with its chat modes and shoutouts; null for everyone
 * else.
 */
internal sealed interface ChatModeControl {
    /** The free build: the commands and the menu's row show a lock, and [showPremium] opens the Premium banner. */
    class Locked(val showPremium: () -> Unit) : ChatModeControl

    /**
     * Makes [change], or gives [shoutout] to the channel of a login; null once it is done, or the error to show when
     * Twitch refuses it or cannot be reached.
     */
    class Available(
        val change: suspend (ChatModeChange) -> ChatSendError?,
        val shoutout: suspend (login: String) -> ChatSendError?,
    ) : ChatModeControl
}
