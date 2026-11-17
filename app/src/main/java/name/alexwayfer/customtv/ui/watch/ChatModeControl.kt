package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.ChatModeChange
import name.alexwayfer.customtv.data.ChatSendError

/**
 * What the streamer or a moderator of the open channel can do with its chat modes, shoutouts and announcements.
 */
internal sealed interface ChatModeControl {
    /** The free build: the commands and the menu's row show a lock, and [showPremium] opens the Premium banner. */
    class Locked(val showPremium: () -> Unit) : ChatModeControl

    /**
     * Makes [change], gives a [shoutout] or uses [announce] to post an announcement. Returns null on success or an
     * error to show.
     */
    class Available(
        val change: suspend (ChatModeChange) -> ChatSendError?,
        val shoutout: suspend (login: String) -> ChatSendError?,
        val announce: suspend (message: String, color: String) -> ChatSendError?,
    ) : ChatModeControl
}
