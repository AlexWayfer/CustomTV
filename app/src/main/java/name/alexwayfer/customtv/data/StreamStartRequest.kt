package name.alexwayfer.customtv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The intent extra a stream-start notification puts the channel login in. */
internal const val STREAM_START_CHANNEL = "stream_start_channel"

/** The intent extra the "log in again" stream-alerts notification sets. */
internal const val STREAM_ALERTS_LOGIN = "stream_alerts_login"

/** The periodic stream-start check. The free build cancels one a Premium install scheduled. */
internal const val STREAM_START_WORK_NAME = "stream-start"

/** A channel a tapped stream-start notification asks the app to open. */
internal object StreamStartRequest {
    private val pending = MutableStateFlow<String?>(null)
    val login: StateFlow<String?> = pending.asStateFlow()

    fun offer(login: String?) {
        val channel = login?.trim()?.takeIf { it.isNotEmpty() } ?: return
        pending.value = channel
    }

    fun clear() {
        pending.value = null
    }
}

/** A tapped "log in again" notification asks the app to start a Twitch login. */
internal object StreamAlertsLoginRequest {
    private val pending = MutableStateFlow(false)
    val requested: StateFlow<Boolean> = pending.asStateFlow()

    fun offer(requested: Boolean) {
        if (requested) pending.value = true
    }

    fun clear() {
        pending.value = false
    }
}
