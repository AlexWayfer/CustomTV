package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.isOwnLiveChatLine
import name.alexwayfer.customtv.chat.slowModeDeadline
import name.alexwayfer.customtv.chat.slowModeDeadlineAfterOwnMessage
import kotlin.time.Duration.Companion.milliseconds

/**
 * Slow mode for the user's own messages. The countdown starts when the user's message shows up in
 * live chat, so a message sent from another device starts it too. A send from this device blocks
 * the next one right away, until its echo arrives or the slow interval passes without one.
 */
@Stable
internal class SlowModeSendState {
    var deadlineMillis by mutableStateOf<Long?>(null)
        internal set
    var awaitingOwnEchoUntilMillis by mutableStateOf<Long?>(null)
        internal set

    fun onSent(slowSeconds: Int?, viewerExempt: Boolean) {
        val seconds = slowSeconds?.takeIf { it > 0 } ?: return
        if (viewerExempt) return
        awaitingOwnEchoUntilMillis = slowModeDeadline(System.currentTimeMillis(), seconds)
    }
}

@Composable
internal fun rememberSlowModeSend(
    channel: String,
    ownUserId: String?,
    liveMessages: Flow<ChatMessage>,
    slowSeconds: Int?,
    viewerExempt: Boolean,
): SlowModeSendState {
    val state = remember(channel) { SlowModeSendState() }
    val readSlowSeconds = rememberUpdatedState(slowSeconds)
    val readViewerExempt = rememberUpdatedState(viewerExempt)
    LaunchedEffect(state, ownUserId, liveMessages) {
        liveMessages.collect { message ->
            if (!isOwnLiveChatLine(message, ownUserId)) return@collect
            state.deadlineMillis = slowModeDeadlineAfterOwnMessage(
                message = message,
                slowSeconds = readSlowSeconds.value,
                viewerExempt = readViewerExempt.value,
                receivedAtMillis = System.currentTimeMillis(),
            )
            state.awaitingOwnEchoUntilMillis = null
        }
    }
    val awaitingUntil = state.awaitingOwnEchoUntilMillis
    LaunchedEffect(state, awaitingUntil) {
        if (awaitingUntil == null) return@LaunchedEffect
        delay((awaitingUntil - System.currentTimeMillis()).coerceAtLeast(0L).milliseconds)
        state.awaitingOwnEchoUntilMillis = null
    }
    return state
}
