package name.alexwayfer.customtv.ui.watch

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChatMessage

/** The free build has no moderator actions in the message menu. */
@Composable
internal fun ProvideMessageMenuModeration(enabled: Boolean, content: @Composable () -> Unit) = content()

@Composable
internal fun ChatMessageModerationRows(message: ChatMessage, onChosen: () -> Unit) = Unit

@Composable
internal fun ModerationFailureSnackbar(snackbarHostState: SnackbarHostState) = Unit
