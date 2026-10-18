package name.alexwayfer.customtv.links

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The intent extra [TwitchLinkActivity] puts a twitch.tv link the app opens in. */
internal const val TWITCH_LINK_URL = "twitch_link_url"

/** A twitch.tv link the app was asked to open, from outside or from a link tapped in the app. */
internal object TwitchLinkRequest {
    private val pending = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = pending.asStateFlow()

    fun offer(url: String?) {
        pending.value = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
    }

    fun clear() {
        pending.value = null
    }
}
