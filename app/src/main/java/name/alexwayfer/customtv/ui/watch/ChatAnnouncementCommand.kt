package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import kotlinx.coroutines.CoroutineScope
import name.alexwayfer.customtv.data.ChatSendError

internal sealed interface ChatAnnouncementCommand {
    data class Send(val message: String, val color: String) : ChatAnnouncementCommand

    data object MissingMessage : ChatAnnouncementCommand
}

/** Reads an announcement command at the start of the draft, including its color and message. */
internal fun chatAnnouncementCommand(text: String): ChatAnnouncementCommand? {
    val trimmed = text.trim()
    val word = trimmed.takeWhile { !it.isWhitespace() }
    val color = ChatCommand.entries.firstOrNull { it.word.equals(word, ignoreCase = true) }?.announcementColor
        ?: return null
    val message = trimmed.substring(word.length).trimStart()
    return if (message.isEmpty()) {
        ChatAnnouncementCommand.MissingMessage
    } else {
        ChatAnnouncementCommand.Send(message, color)
    }
}

internal fun ChatComposerState.runAnnouncementCommand(
    command: ChatAnnouncementCommand,
    control: ChatModeControl?,
    text: String,
    draft: TextFieldState,
    scope: CoroutineScope,
) {
    if (sending) return
    sendError = when {
        control == null -> ChatSendError.AnnouncementNotAllowed
        control is ChatModeControl.Locked -> ChatSendError.AnnouncementPremium
        command is ChatAnnouncementCommand.MissingMessage -> ChatSendError.AnnouncementUsage
        else -> null
    }
    if (control is ChatModeControl.Available && command is ChatAnnouncementCommand.Send) {
        startSend(scope) {
            val error = control.announce(command.message, command.color)
            if (error != null) {
                sendError = error
            } else if (draft.text.toString() == text) {
                draft.clearText()
            }
        }
    }
}
