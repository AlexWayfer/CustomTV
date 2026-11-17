package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal val ACTIVITY_STATISTICS_ENABLED = booleanPreferencesKey("activity_statistics_enabled")

/** The opt-out is backed up with settings; installation identities and keys must not be. */
internal fun activityStatisticsEnabled(preferences: Preferences): Boolean =
    preferences[ACTIVITY_STATISTICS_ENABLED] ?: true

internal class ActivityStatisticsPreferences(context: Context) {
    private val store = context.applicationContext.appSettingsDataStore
    val enabled: Flow<Boolean> = store.data.map(::activityStatisticsEnabled).distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[ACTIVITY_STATISTICS_ENABLED] = enabled }
    }
}
