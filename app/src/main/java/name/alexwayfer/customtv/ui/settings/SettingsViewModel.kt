package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.ChatPageSettingsStore
import name.alexwayfer.customtv.data.MentionPageSettingsStore
import name.alexwayfer.customtv.data.PortraitLanguage
import name.alexwayfer.customtv.data.PortraitModelKind

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppSettingsStore(application)

    val settings: StateFlow<AppSettings?> = AppSettingsStore.restored
    val chat = ChatPageSettingsActions(viewModelScope, ChatPageSettingsStore(application))
    val mentions = MentionPageSettingsActions(viewModelScope, MentionPageSettingsStore(application))

    fun setBackgroundSoundOnly(enabled: Boolean) {
        viewModelScope.launch { store.setBackgroundSoundOnly(enabled) }
    }

    fun setAutoClaimBonus(enabled: Boolean) {
        viewModelScope.launch {
            store.setAutoClaimBonus(enabled)
        }
    }

    fun setStreamStartNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setStreamStartNotifications(enabled)
        }
    }

    fun setStreamChangeNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setStreamChangeNotifications(enabled)
        }
    }

    fun setWatchStreakNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.setWatchStreakNotifications(enabled)
        }
    }

    fun setPortraitLanguage(language: PortraitLanguage) {
        viewModelScope.launch {
            store.setPortraitLanguage(language)
        }
    }

    fun setPortraitModel(model: PortraitModelKind) {
        viewModelScope.launch {
            store.setPortraitModel(model)
        }
    }
}
