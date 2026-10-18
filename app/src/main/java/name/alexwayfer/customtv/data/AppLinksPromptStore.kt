package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the twitch.tv links card on Home was put away with Not now, next to the app settings. */
internal class AppLinksPromptStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    val dismissed: Flow<Boolean> = dataStore.data.map { it[DISMISSED] ?: false }

    suspend fun markDismissed() {
        dataStore.edit { it[DISMISSED] = true }
    }

    private companion object {
        val DISMISSED = booleanPreferencesKey("twitch_links_prompt_dismissed")
    }
}
