package name.alexwayfer.customtv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.ChatPageSettingsStore
import name.alexwayfer.customtv.data.HiddenMessageSettingsStore
import name.alexwayfer.customtv.data.MentionPageSettingsStore
import name.alexwayfer.customtv.data.PortraitLanguage
import name.alexwayfer.customtv.data.PortraitModelKind

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppSettingsStore(application)

    val settings: StateFlow<AppSettings?> = AppSettingsStore.restored
    val chat = ChatPageSettingsActions(viewModelScope, ChatPageSettingsStore(application))
    val mentions = MentionPageSettingsActions(viewModelScope, MentionPageSettingsStore(application))
    val hiddenMessages = HiddenMessageSettingsActions(viewModelScope, HiddenMessageSettingsStore(application))

    private var labelsEditor: Pair<String, ChatterLabelsEditorState>? = null

    /**
     * The labels page's editor for one [opening] of the page. It stays the same while the page's saved state lives
     * on, as over a profile opened from it, so the edits and the Gist the page read stay too; it holds images too
     * large for saved state. A new opening gets a new editor, which reads the Gist again.
     */
    internal fun chatterLabelsEditor(opening: String): ChatterLabelsEditorState {
        labelsEditor?.let { (key, editor) -> if (key == opening) return editor }
        return ChatterLabelsEditorState().also { labelsEditor = opening to it }
    }

    fun setBackgroundSoundOnly(enabled: Boolean) {
        viewModelScope.launch { store.setBackgroundSoundOnly(enabled) }
    }

    fun setSmoothSound(enabled: Boolean) {
        viewModelScope.launch { store.setSmoothSound(enabled) }
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

    fun setWhisperUnreadGlow(enabled: Boolean) {
        viewModelScope.launch { store.setWhisperUnreadGlow(enabled) }
    }

    fun setLiveWidgetEnded(enabled: Boolean) {
        viewModelScope.launch { store.setLiveWidgetEnded(enabled) }
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
