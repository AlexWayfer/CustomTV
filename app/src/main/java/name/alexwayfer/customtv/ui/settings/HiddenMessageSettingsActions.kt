package name.alexwayfer.customtv.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.HiddenMessageSettingsStore
import name.alexwayfer.customtv.data.KeywordPhrase

/** Saves what the user changes in the hidden messages section of the Chat page. */
class HiddenMessageSettingsActions(private val scope: CoroutineScope, private val store: HiddenMessageSettingsStore) {
    fun setHiddenPhrases(phrases: List<KeywordPhrase>) {
        scope.launch { store.setHiddenPhrases(phrases) }
    }

    fun setHidePhrasesWhenModerator(enabled: Boolean) {
        scope.launch { store.setHidePhrasesWhenModerator(enabled) }
    }
}
