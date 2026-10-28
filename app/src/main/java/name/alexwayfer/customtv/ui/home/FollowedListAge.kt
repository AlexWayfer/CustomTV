package name.alexwayfer.customtv.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * When the shown follow list was loaded, by the wall clock as the saved file's time is, and whether it is too old to
 * pass for the current one.
 */
internal class FollowedListAge {
    private val _stale = MutableStateFlow(false)
    /** The shown list is older than [FOLLOWED_LIST_STALE_AFTER_MS] and no load has replaced it yet. */
    val stale: StateFlow<Boolean> = _stale
    private var shownAtMillis: Long? = null

    /** A list saved at [savedAtMillis] is shown in place of none. */
    fun showSaved(savedAtMillis: Long) {
        shownAtMillis = savedAtMillis
        check()
    }

    /** A load has just replaced the shown list. */
    fun showLoaded() {
        shownAtMillis = System.currentTimeMillis()
        _stale.value = false
    }

    /** No list is shown. */
    fun clear() {
        shownAtMillis = null
        _stale.value = false
    }

    /** Dims the shown list once it has grown too old, such as one kept in memory while the app was away overnight. */
    fun check() {
        if (followedListStale(shownAtMillis, System.currentTimeMillis())) _stale.value = true
    }
}
