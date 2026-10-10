package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.chatRulesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chat_rules",
)

/**
 * The chat rules the user has confirmed, as [chatRulesFingerprint] per channel ID, so a renamed
 * channel keeps its confirmation and edited rules show again.
 */
class ChatRulesStore(context: Context) {
    private val dataStore = context.applicationContext.chatRulesDataStore

    suspend fun acknowledgedFingerprint(channelId: String): String? {
        val key = fingerprintKey(channelId)
        return dataStore.data.map { prefs -> prefs[key] }.first()
    }

    suspend fun acknowledge(channelId: String, fingerprint: String) {
        if (channelId.isBlank()) return
        dataStore.edit { prefs -> prefs[fingerprintKey(channelId)] = fingerprint }
    }

    private fun fingerprintKey(channelId: String) = stringPreferencesKey("rules_${channelId.trim()}")
}
