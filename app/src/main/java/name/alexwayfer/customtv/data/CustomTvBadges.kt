package name.alexwayfer.customtv.data

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/** The users with a CustomTV badge, loaded from the Mecena server while the switch is on. */
internal object CustomTvBadges {
    private val _list = MutableStateFlow(CustomTvBadgeList())
    val list: StateFlow<CustomTvBadgeList> = _list.asStateFlow()

    private val lock = Mutex()
    private var loadedAtMillis: Long? = null
    private var etag: String? = null

    /**
     * Loads the list when the last load is [CUSTOM_TV_BADGES_RELOAD] old or there was none, and tells how long until
     * it is due again. A failed load tries again with the next chat, or after [CUSTOM_TV_BADGES_RELOAD] in the open
     * one.
     */
    private suspend fun load(): Duration = lock.withLock {
        val now = SystemClock.elapsedRealtime()
        if (customTvBadgesNextLoadIn(loadedAtMillis, now) == Duration.ZERO) {
            when (val answer = CustomTvBadgeApi.list(etag)) {
                is CustomTvBadgeListAnswer.Changed -> {
                    _list.value = answer.list
                    etag = answer.etag
                }
                CustomTvBadgeListAnswer.Unchanged -> Unit
                CustomTvBadgeListAnswer.Failed -> return@withLock CUSTOM_TV_BADGES_RELOAD
            }
            loadedAtMillis = now
        }
        customTvBadgesNextLoadIn(loadedAtMillis, now)
    }

    /** Keeps the list fresh while the caller runs, such as an open chat on screen. */
    suspend fun keepLoaded(): Nothing {
        while (true) delay(load())
    }

    /**
     * Shows the user's own badge as the server just set it, without waiting for the next load. It waits for a load in
     * flight, whose older list would hide the change.
     */
    suspend fun showOwn(twitchUserId: String, edition: CustomTvBadgeEdition?) = lock.withLock {
        _list.update { it.with(twitchUserId, edition) }
    }
}
