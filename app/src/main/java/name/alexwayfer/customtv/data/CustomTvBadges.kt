package name.alexwayfer.customtv.data

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The users with a CustomTV badge, loaded from the Mecena server while the switch is on. */
internal object CustomTvBadges {
    private val _list = MutableStateFlow(CustomTvBadgeList())
    val list: StateFlow<CustomTvBadgeList> = _list.asStateFlow()

    private val lock = Mutex()
    private var loadedAtMillis: Long? = null
    private var etag: String? = null

    /**
     * Loads the list with the first chat of a run, and again with a chat opened [CUSTOM_TV_BADGES_RELOAD] after the
     * last load; a failed load tries again with the next chat.
     */
    suspend fun load() = lock.withLock {
        val now = SystemClock.elapsedRealtime()
        if (!customTvBadgesDue(loadedAtMillis, now)) return@withLock
        when (val answer = CustomTvBadgeApi.list(etag)) {
            is CustomTvBadgeListAnswer.Changed -> {
                _list.value = answer.list
                etag = answer.etag
            }
            CustomTvBadgeListAnswer.Unchanged -> Unit
            CustomTvBadgeListAnswer.Failed -> return@withLock
        }
        loadedAtMillis = now
    }

    /**
     * Shows the user's own badge as the server just set it, without waiting for the next load. It waits for a load in
     * flight, whose older list would hide the change.
     */
    suspend fun showOwn(twitchUserId: String, edition: CustomTvBadgeEdition?) = lock.withLock {
        _list.update { it.with(twitchUserId, edition) }
    }
}
