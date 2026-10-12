package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration.Companion.seconds

/** How long the chat waits before it connects again after losing its connection. */
internal val CHAT_RECONNECT_DELAY = 1.seconds

/**
 * Connects the chat to its channel again after a pause, once the connection was lost. Only the latest one waits: a
 * new one, or [cancel] on a new connect or when the screen leaves, replaces it. A channel opened meanwhile is not
 * reconnected to the old one.
 */
internal class ChatReconnect(
    private val scope: CoroutineScope,
    private val currentChannel: () -> String?,
    private val connect: (channel: String) -> Unit,
    private val wait: suspend () -> Unit = { delay(CHAT_RECONNECT_DELAY) },
) {
    private var job: Job? = null

    fun schedule() {
        val channel = currentChannel() ?: return
        job?.cancel()
        AppLog.i(CHAT_IRC_TAG, "retry in ${CHAT_RECONNECT_DELAY.inWholeMilliseconds}ms")
        job = scope.launch {
            wait()
            if (currentChannel() == channel) connect(channel)
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
