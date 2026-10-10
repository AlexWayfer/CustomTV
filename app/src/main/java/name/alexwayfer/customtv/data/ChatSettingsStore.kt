package name.alexwayfer.customtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.chat.chatTextSizeFromStored
import name.alexwayfer.customtv.chat.smoothChatScrollFromStored
import name.alexwayfer.customtv.diagnostics.AppLog

private val Context.chatSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chat_settings",
)

data class ChatSettings(
    val readableColors: Boolean = true,
    val timestamps: Boolean = false,
    val textSize: Int = chatTextSizeFromStored(null),
    val smoothChatScroll: Boolean = smoothChatScrollFromStored(null),
    val fullscreenChatMode: FullscreenChatMode = fullscreenChatModeFromStored(null),
    val fullscreenChatSide: FullscreenChatSide = fullscreenChatSideFromStored(null),
)

class ChatSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.chatSettingsDataStore

    val settings: Flow<ChatSettings> = dataStore.data.map { prefs ->
        val defaults = ChatSettings()
        ChatSettings(
            readableColors = prefs[READABLE_COLORS] ?: defaults.readableColors,
            timestamps = prefs[TIMESTAMPS] ?: defaults.timestamps,
            textSize = chatTextSizeFromStored(prefs[TEXT_SIZE]),
            smoothChatScroll = smoothChatScrollFromStored(prefs[SMOOTH_CHAT_SCROLL]),
            fullscreenChatMode = fullscreenChatModeFromStored(prefs[FULLSCREEN_CHAT_MODE]),
            fullscreenChatSide = fullscreenChatSideFromStored(prefs[FULLSCREEN_CHAT_SIDE]),
        )
    }

    suspend fun setReadableColors(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[READABLE_COLORS] = enabled
        }
    }

    suspend fun setTimestamps(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TIMESTAMPS] = enabled
        }
    }

    suspend fun setTextSize(size: Int) {
        dataStore.edit { prefs ->
            prefs[TEXT_SIZE] = chatTextSizeFromStored(size)
        }
    }

    suspend fun setSmoothChatScroll(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SMOOTH_CHAT_SCROLL] = enabled
        }
    }

    suspend fun setFullscreenChatMode(mode: FullscreenChatMode) {
        dataStore.edit { prefs ->
            prefs[FULLSCREEN_CHAT_MODE] = mode.stored
        }
    }

    suspend fun setFullscreenChatSide(side: FullscreenChatSide) {
        dataStore.edit { prefs ->
            prefs[FULLSCREEN_CHAT_SIDE] = side.stored
        }
    }

    companion object {
        private const val TAG = "ChatSettings"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val current = MutableStateFlow<ChatSettings?>(null)

        /** Null until DataStore emits once, so a switch can wait for the saved value. */
        val restored: StateFlow<ChatSettings?> = current.asStateFlow()
        private var reading = false

        fun start(context: Context) {
            if (reading) return
            reading = true
            val store = ChatSettingsStore(context.applicationContext)
            scope.launch {
                try {
                    store.settings.collect { current.value = it }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    AppLog.w(TAG, "settings restore failed: ${failure.javaClass.simpleName}")
                }
            }
        }

        private val READABLE_COLORS = booleanPreferencesKey("readable_colors")
        private val TIMESTAMPS = booleanPreferencesKey("timestamps")
        private val TEXT_SIZE = intPreferencesKey("text_size")
        private val SMOOTH_CHAT_SCROLL = booleanPreferencesKey("adaptive_smooth_chat_enabled")
        private val FULLSCREEN_CHAT_MODE = stringPreferencesKey("fullscreen_chat_mode")
        private val FULLSCREEN_CHAT_SIDE = stringPreferencesKey("fullscreen_chat_side")
    }
}
