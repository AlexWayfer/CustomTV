package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.data.ChatSendError
import name.alexwayfer.customtv.data.chatMessageToSend

/**
 * Something the message field can post in place of a plain message, such as a watch streak with the user's words.
 * [note] shows above the field: first the offer, then, once the user chose to share ([composing]), a line that says
 * what Send does. Only while composing does Send post through [share]; null there means posted. [focusRequest]
 * opens the keyboard once when composing starts. An offer that is not composing may put [prefill] in the field once,
 * such as an unlocked emote, and hears through [onPlainSent] that a plain message went out.
 */
internal class ChatShareOffer(
    val note: @Composable () -> Unit,
    val composing: Boolean,
    val focusRequest: OneShotRequest<Unit>?,
    /** A streak may go without words; a reward's message may not. */
    val emptyAllowed: Boolean,
    val share: suspend (message: String) -> ChatSendError?,
    val prefill: OneShotRequest<String>? = null,
    val onPlainSent: (() -> Unit)? = null,
)

/** The field's text with [shared] added at its end, a space apart, and a space after it for what comes next. */
internal fun draftWithShared(draft: String, shared: String): String = when {
    draft.isBlank() -> "$shared "
    draft.last().isWhitespace() -> "$draft$shared "
    else -> "$draft $shared "
}

/**
 * The text Send posts: a plain message needs words, while a share that allows it may go with none, as a streak
 * does on the Twitch web. Null when there is nothing to post.
 */
internal fun chatTextToSubmit(raw: String, emptyAllowed: Boolean): String? =
    chatMessageToSend(raw) ?: if (emptyAllowed) "" else null
