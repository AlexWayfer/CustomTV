package name.alexwayfer.customtv.ui.watch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChatSettings
import name.alexwayfer.customtv.data.ChatSettingsStore
import name.alexwayfer.customtv.data.FullscreenChatMode
import name.alexwayfer.customtv.data.FullscreenChatSide

class ChatSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ChatSettingsStore(application)

    /** Read when the application starts; null only until the first DataStore emission. */
    val settings: StateFlow<ChatSettings?> = ChatSettingsStore.restored

    fun setReadableColors(enabled: Boolean) {
        viewModelScope.launch {
            store.setReadableColors(enabled)
        }
    }

    fun setTimestamps(enabled: Boolean) {
        viewModelScope.launch {
            store.setTimestamps(enabled)
        }
    }

    fun setTextSize(size: Int) {
        viewModelScope.launch {
            store.setTextSize(size)
        }
    }

    fun setSmoothChatScroll(scroll: Boolean) {
        viewModelScope.launch {
            store.setSmoothChatScroll(scroll)
        }
    }

    fun setFullscreenChatMode(mode: FullscreenChatMode) {
        viewModelScope.launch {
            store.setFullscreenChatMode(mode)
        }
    }

    fun setFullscreenChatSide(side: FullscreenChatSide) {
        viewModelScope.launch {
            store.setFullscreenChatSide(side)
        }
    }
}
