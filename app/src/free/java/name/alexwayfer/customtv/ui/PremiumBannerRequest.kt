package name.alexwayfer.customtv.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/**
 * Shows the Premium banner at once, out of its schedule: from a Premium feature the free build shows locked. Such a
 * show counts as one and starts the wait over, so the banner does not come again on its own a day later.
 */
internal object PremiumBannerRequest {
    private val pending = MutableStateFlow<OneShotRequest<String>?>(null)
    val requests: StateFlow<OneShotRequest<String>?> = pending.asStateFlow()

    /** [source] names where the tap came from, for the log. */
    fun send(source: String) {
        pending.value = OneShotRequest(source)
    }
}
