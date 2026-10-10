package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendResult

internal fun chatSendReturnsToLatest(result: ChatSendResult, draftMatchesSentMessage: Boolean): Boolean =
    result == ChatSendResult.Sent && draftMatchesSentMessage

/**
 * Whether Send hides the keyboard at the tap, while the message is still on its way.
 * The keyboard does not come back after a failed send either way: the error shows under the field.
 */
internal fun chatSendHidesKeyboard(keepKeyboardAfterSend: Boolean): Boolean = !keepKeyboardAfterSend
