package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog

private val Context.recentChannelsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recent_channels",
)

/**
 * The recent channels, keyed by channel ID so a renamed channel stays one entry. The login is
 * kept only to show and open the channel before [RecentChannelsStore.updateLogins] refreshes it.
 */
data class RecentChannel(
    val id: String,
    val login: String,
)

class RecentChannelsStore(context: Context) {
    private val dataStore = context.applicationContext.recentChannelsDataStore

    val recents: Flow<List<RecentChannel>> = dataStore.data.map { prefs ->
        parseRecentChannels(prefs[KEY].orEmpty())
    }

    suspend fun snapshot(): List<RecentChannel> = recents.first()

    suspend fun add(channel: RecentChannel) {
        dataStore.edit { prefs ->
            val current = parseRecentChannels(prefs[KEY].orEmpty())
            prefs[KEY] = recentChannelsJoined(recentChannelsAfterAdd(current, channel))
        }
    }

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            val current = parseRecentChannels(prefs[KEY].orEmpty())
            prefs[KEY] = recentChannelsJoined(current.filter { it.id != id })
        }
    }

    /** Takes the current login of each channel ID in [logins]. */
    suspend fun updateLogins(logins: Map<String, String>) {
        if (logins.isEmpty()) return
        dataStore.edit { prefs ->
            val current = parseRecentChannels(prefs[KEY].orEmpty())
            val updated = recentChannelsWithLogins(current, logins)
            if (updated != current) prefs[KEY] = recentChannelsJoined(updated)
        }
    }

    companion object {
        private const val TAG = "RecentChannels"
        private val KEY = stringPreferencesKey("channels")
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val current = MutableStateFlow<List<RecentChannel>?>(null)

        /** The recents read when the app starts and kept current; null until the first read. */
        val restored: StateFlow<List<RecentChannel>?> = current.asStateFlow()
        private var reading = false

        fun start(context: Context) {
            if (reading) return
            reading = true
            val store = RecentChannelsStore(context.applicationContext)
            scope.launch {
                try {
                    store.recents.collect { current.value = it }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    AppLog.w(TAG, "recents restore failed: ${failure.javaClass.simpleName}")
                }
            }
        }
    }
}

private const val MAX_RECENTS = 50

/** One `id login` line per channel. A line without both parts, such as a bare login, is skipped. */
internal fun parseRecentChannels(raw: String): List<RecentChannel> {
    return raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.trim().split(' ')
            if (parts.size != 2) return@mapNotNull null
            val id = parts[0].trim()
            val login = parts[1].trim().lowercase()
            if (id.isEmpty() || login.isEmpty()) null else RecentChannel(id, login)
        }
        .distinctBy { it.id }
        .toList()
}

internal fun recentChannelsJoined(channels: List<RecentChannel>): String =
    channels.joinToString("\n") { "${it.id} ${it.login}" }

internal fun recentChannelsAfterAdd(current: List<RecentChannel>, added: RecentChannel): List<RecentChannel> {
    val channel = RecentChannel(added.id.trim(), added.login.trim().lowercase())
    if (channel.id.isEmpty() || channel.login.isEmpty()) return current
    return (listOf(channel) + current.filter { it.id != channel.id }).take(MAX_RECENTS)
}

internal fun recentChannelsWithLogins(
    current: List<RecentChannel>,
    logins: Map<String, String>,
): List<RecentChannel> {
    return current.map { channel ->
        val login = logins[channel.id]?.trim()?.lowercase()
        if (login.isNullOrEmpty() || login == channel.login) channel else channel.copy(login = login)
    }
}
