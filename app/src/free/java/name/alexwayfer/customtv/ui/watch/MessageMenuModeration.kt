package name.alexwayfer.customtv.ui.watch

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.chat.ChatMessage

/** The free build has no moderator actions in the message menu or on a swipe. */
@Composable
internal fun ProvideMessageMenuModeration(
    enabled: Boolean,
    swipeEnabled: Boolean,
    content: @Composable () -> Unit,
) = content()

@Composable
internal fun ChatMessageModerationRows(message: ChatMessage, onChosen: () -> Unit) = Unit

/** A swipe toward the end of a message copies it in the free build. */
@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun rememberChatSwipeModeration(message: ChatMessage): ChatSwipeSteps? = null

@Composable
internal fun ModerationFailureSnackbar(snackbarHostState: SnackbarHostState) = Unit
