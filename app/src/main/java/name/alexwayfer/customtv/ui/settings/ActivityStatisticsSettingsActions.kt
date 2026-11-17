package name.alexwayfer.customtv.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ActivityStatisticsPreferences

/** Keep a pending settings write alive when the user leaves the main settings page. */
internal class ActivityStatisticsSettingsActions(
    private val scope: CoroutineScope,
    private val preferences: ActivityStatisticsPreferences,
) {
    val enabled = preferences.enabled

    fun setEnabled(enabled: Boolean) {
        scope.launch { preferences.setEnabled(enabled) }
    }
}
