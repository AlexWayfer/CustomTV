package name.alexwayfer.customtv.ui.watch

internal enum class ChatReplyComposer { Keyboard, Rules, Nothing }

/**
 * What choosing Reply opens below the thread. A signed-in viewer gets the message field with the
 * keyboard, or the chat rules first when they still need confirming; confirming them then brings the
 * keyboard. Logged out, the thread opens alone: the field would only lead to Log in. A warning to acknowledge
 * first has taken the field's place, so the thread opens alone then too.
 */
internal fun chatReplyComposer(
    canSend: Boolean,
    warned: Boolean,
    rulesNeedConfirmation: Boolean,
): ChatReplyComposer = when {
    !canSend || warned -> ChatReplyComposer.Nothing
    rulesNeedConfirmation -> ChatReplyComposer.Rules
    else -> ChatReplyComposer.Keyboard
}
