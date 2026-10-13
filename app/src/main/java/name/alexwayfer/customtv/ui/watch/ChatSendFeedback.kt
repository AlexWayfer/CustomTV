package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendResult

/** Whether the send went through: the draft clears and an open thread closes. */
internal fun chatSendCompleted(result: ChatSendResult, draftMatchesSentMessage: Boolean): Boolean =
    result == ChatSendResult.Sent && draftMatchesSentMessage

/**
 * Whether the chat follows new messages after a send. A chat that follows keeps following; a chat the viewer
 * stopped jumps to the newest message only with the setting on.
 */
internal fun chatFollowsAfterSend(following: Boolean, scrollToLatestAfterSend: Boolean): Boolean =
    following || scrollToLatestAfterSend

/**
 * Whether Send hides the keyboard at the tap, while the message is still on its way.
 * The keyboard does not come back after a failed send either way: the error shows under the field.
 */
internal fun chatSendHidesKeyboard(keepKeyboardAfterSend: Boolean): Boolean = !keepKeyboardAfterSend
