package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the notifications card on Home was answered, next to the app settings. */
internal class NotificationPromptStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    val dismissed: Flow<Boolean> = dataStore.data.map { it[DISMISSED] ?: false }

    suspend fun markDismissed() {
        dataStore.edit { it[DISMISSED] = true }
    }

    suspend fun clearDismissed() {
        dataStore.edit { it.remove(DISMISSED) }
    }

    private companion object {
        val DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
    }
}
