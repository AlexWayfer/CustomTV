package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Keeps an edited chat draft within `maxLength` by cutting the text past it. Unlike
 * `InputTransformation.maxLength`, a paste that is too long is shortened instead of dropped.
 * The buffer clips the selection and the keyboard's composing region itself, so keyboards such
 * as SwiftKey keep the word being typed.
 */
internal class ChatDraftLimit(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength) replace(maxLength, length, "")
    }
}
