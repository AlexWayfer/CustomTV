package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import name.alexwayfer.customtv.chat.ChatWarning

/**
 * The warning that keeps the user from chatting in the open channel, for the plaque in the field's place. A refused
 * send tells of one in every build. The premium build with the TV login also learns of it ahead, follows it live,
 * and acknowledges it through [acknowledgeOnTwitch]; without that the user acknowledges it on the Twitch web.
 */
@Stable
internal class ChatWarningGate(
    private val acknowledgeOnTwitch: (suspend () -> Boolean)?,
    private val onSendRefused: () -> Unit = {},
) {
    var warning by mutableStateOf<ChatWarning?>(null)
        private set
    var acknowledging by mutableStateOf(false)
        private set
    var acknowledgeFailed by mutableStateOf(false)
        private set

    val canAcknowledge: Boolean
        get() = acknowledgeOnTwitch != null

    fun show(warning: ChatWarning) {
        this.warning = warning
        acknowledgeFailed = false
    }

    fun clear() {
        warning = null
        acknowledgeFailed = false
    }

    /** Twitch dropped a message as warned: the plaque shows at once, and the build loads the details if it can. */
    fun sendRefused() {
        if (warning == null) warning = ChatWarning()
        onSendRefused()
    }

    suspend fun acknowledge() {
        val send = acknowledgeOnTwitch ?: return
        if (acknowledging) return
        acknowledging = true
        acknowledgeFailed = false
        val done = send()
        acknowledging = false
        if (done) clear() else acknowledgeFailed = true
    }
}
