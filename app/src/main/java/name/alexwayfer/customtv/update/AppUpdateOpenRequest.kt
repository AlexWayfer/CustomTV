package name.alexwayfer.customtv.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The intent extra a tapped update notification sets. */
internal const val APP_UPDATE_OPEN = "app_update_open"

/** A tapped update notification asks the app to show the Updates section of the settings. */
internal object AppUpdateOpenRequest {
    private val pending = MutableStateFlow(false)
    val requested: StateFlow<Boolean> = pending.asStateFlow()

    fun offer(requested: Boolean) {
        if (requested) pending.value = true
    }

    fun clear() {
        pending.value = false
    }
}
