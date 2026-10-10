package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/** When the Premium banner was last shown and how many players opened since, next to the app settings. */
internal class PremiumBannerStore(context: Context) {
    private val dataStore = context.applicationContext.appSettingsDataStore

    suspend fun shownAtMillis(): Long? = dataStore.data.first()[SHOWN_AT]

    suspend fun playerOpens(): Int = dataStore.data.first()[PLAYER_OPENS] ?: 0

    /** Counts up to [cap]: past it the count no longer changes when the banner is due. */
    suspend fun countPlayerOpen(cap: Int) {
        dataStore.edit { prefs ->
            val opens = prefs[PLAYER_OPENS] ?: 0
            if (opens < cap) prefs[PLAYER_OPENS] = opens + 1
        }
    }

    /** A show starts the wait over: the interval and the opens count from it. */
    suspend fun markShown(millis: Long) {
        dataStore.edit { prefs ->
            prefs[SHOWN_AT] = millis
            prefs[PLAYER_OPENS] = 0
        }
    }

    private companion object {
        val SHOWN_AT = longPreferencesKey("premium_banner_shown_at")
        val PLAYER_OPENS = intPreferencesKey("premium_banner_player_opens")
    }
}
