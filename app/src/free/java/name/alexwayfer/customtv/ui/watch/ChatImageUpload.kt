package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal class ChatImageUpload

@Composable
internal fun ChatComposerPickers(
    composer: ChatComposerState,
    compose: ChatCompose,
    showAttachment: Boolean,
    attachmentEnabled: Boolean,
    emotes: @Composable () -> Unit,
) = emotes()

@Composable
internal fun ChatImageUploadFeedback(upload: ChatImageUpload, draft: TextFieldState) = Unit

@Composable
internal fun Modifier.chatImagePaste(
    composer: ChatComposerState,
    compose: ChatCompose,
    enabled: Boolean,
): Modifier = this
