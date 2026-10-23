package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Deletes an emote shown as a picture whole: a backspace that removes the whitespace after it
 * removes its code too, so one press takes the picture away instead of turning it back into text.
 */
internal class ChatDraftEmoteBackspace(private val output: ChatDraftEmoteOutput) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val code = chatDraftWholeEmoteDeletion(
            original = originalText,
            originalSelection = originalSelection,
            edited = asCharSequence(),
            shown = output.shownEmotes(originalText),
        ) ?: return
        replace(code.start, code.end, "")
    }
}
