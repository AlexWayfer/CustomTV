package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The free build learns of a warning only when Twitch drops a message for it, and the user acknowledges it on the
 * Twitch web: acknowledging in the app takes the premium build's TV login.
 */
@Composable
internal fun rememberChatWarningGate(channelId: String?, userId: String?): ChatWarningGate =
    remember(channelId, userId) { ChatWarningGate(acknowledgeOnTwitch = null) }
