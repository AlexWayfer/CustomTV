package name.alexwayfer.customtv.ui.home

import android.app.Application
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.data.FOLLOWED_CHANNELS_TAG
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.FollowedChannelsAtStart
import name.alexwayfer.customtv.data.FollowedChannelsCache
import name.alexwayfer.customtv.data.FollowedChannelsRepository
import name.alexwayfer.customtv.data.FollowedChannelsResult
import name.alexwayfer.customtv.data.StreamStartAlertsController
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.account.TwitchUserAccess
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** The followed channels Home shows: the list saved at the last start, then each load for the signed-in account. */
internal class HomeFollows(private val application: Application, private val scope: CoroutineScope) {
    private val repository = FollowedChannelsRepository(BuildConfig.TWITCH_CLIENT_ID)
    private val cache = FollowedChannelsCache(application)
    private val _channels = MutableStateFlow<List<FollowedChannel>>(emptyList())
    val channels: StateFlow<List<FollowedChannel>> = _channels
    private val _fromCache = MutableStateFlow(false)
    /** The shown follow list is the one saved by the last start, not a fresh load yet. */
    val fromCache: StateFlow<Boolean> = _fromCache
    private val _status = MutableStateFlow(FollowedChannelsStatus.Loading)
    val status: StateFlow<FollowedChannelsStatus> = _status
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing
    private val _loadFailed = MutableStateFlow<OneShotRequest<Unit>?>(null)
    /** A whole load failed and left the list on screen out of date; the screen tells it once. */
    val loadFailed: StateFlow<OneShotRequest<Unit>?> = _loadFailed
    private var request = false
    private var job: Job? = null
    private var lastRefreshAtMillis: Long? = null
    private var lastUserId: String? = null

    /** The last whole load failed, so the shown list may be out of date and the next load reads it whole. */
    private var lastFullFailed = false

    init {
        scope.launch { showSavedAtStart() }
    }

    fun refresh(
        accessToken: String,
        userId: String,
        liveOnly: Boolean,
        automatic: Boolean = false,
    ) {
        if (followedListBelongsToAnotherAccount(lastUserId, userId)) forget()
        val now = SystemClock.elapsedRealtime()
        if (refreshSkipped(userId, liveOnly, automatic, now)) return
        lastRefreshAtMillis = now
        lastUserId = userId
        val current = _channels.value
        val full = followedLoadIsFull(liveOnly, current.isEmpty(), lastFullFailed)
        val failedBefore = lastFullFailed
        val userAsked = !liveOnly && !automatic
        request = true
        if (full) _refreshing.value = true
        if (current.isEmpty()) _status.value = FollowedChannelsStatus.Loading
        AppLog.i(
            FOLLOWED_CHANNELS_TAG,
            "load ${if (full) "full" else "live"}, showing ${current.size}" +
                if (_fromCache.value) " saved" else "",
        )
        job = scope.launch {
            try {
                if (current.isEmpty()) showCached(userId)
                val result = if (full) {
                    loadFull(accessToken, userId).also { lastFullFailed = it == FollowedChannelsResult.Unavailable }
                } else {
                    withContext(Dispatchers.IO) { repository.refreshLive(accessToken, current) }
                }
                applyResult(result, accessToken, userId)
                val listShown = _channels.value.isNotEmpty()
                if (full && followedLoadFailureShown(result, listShown, failedBefore, userAsked)) {
                    _loadFailed.value = OneShotRequest(Unit)
                }
            } finally {
                if (job === coroutineContext[Job]) {
                    request = false
                    _refreshing.value = false
                }
            }
        }
    }

    /**
     * A load already runs, Twitch refused the login so only a whole load may try again, or an automatic load comes
     * too soon after the last one.
     */
    private fun refreshSkipped(userId: String, liveOnly: Boolean, automatic: Boolean, now: Long): Boolean {
        val status = _status.value
        val loginRefused = status == FollowedChannelsStatus.NeedsPermission || status == FollowedChannelsStatus.Rejected
        return request ||
            liveOnly && loginRefused ||
            automatic && !followedAutomaticRefreshAllowed(lastRefreshAtMillis, now, lastUserId, userId)
    }

    /** The whole list, shown as it comes in; a failure that may pass is tried once more right away. */
    private suspend fun loadFull(accessToken: String, userId: String): FollowedChannelsResult {
        val first = repository.load(accessToken, userId, ::showLoaded)
        if (!followedLoadRetries(first)) return first
        AppLog.i(FOLLOWED_CHANNELS_TAG, "load again")
        return repository.load(accessToken, userId, ::showLoaded)
    }

    /** A part of the whole list that came in: it replaces an empty list, and the shown one waits for the end. */
    private fun showLoaded(channels: List<FollowedChannel>) {
        val next = followedListDuringReload(
            _channels.value,
            channels,
            reloadFinished = false,
        )
        if (next !== _channels.value) {
            _channels.value = followedWithPreviews(
                _channels.value,
                next,
                System.currentTimeMillis(),
            )
            _status.value = FollowedChannelsStatus.Ready
        }
    }

    /** Loads the whole list again, as the user asked: after a follow in the app, or with Retry after a failed load. */
    fun reload(access: suspend () -> TwitchUserAccess?) {
        scope.launch {
            val current = access() ?: return@launch
            refresh(current.accessToken, current.userId, liveOnly = false)
        }
    }

    /** Drops the list of the previous account, and its load still in flight, before the new account loads its own. */
    private fun forget() {
        job?.cancel()
        job = null
        request = false
        _refreshing.value = false
        _fromCache.value = false
        _channels.value = emptyList()
        _status.value = FollowedChannelsStatus.Loading
        lastRefreshAtMillis = null
        lastUserId = null
        lastFullFailed = false
    }

    fun markUnavailable() {
        if (_channels.value.isEmpty()) {
            _status.value = FollowedChannelsStatus.Unavailable
        }
    }

    /** Opens Home with the list read at app start, unless a load has already begun. */
    private suspend fun showSavedAtStart() {
        val saved = FollowedChannelsAtStart.take() ?: return
        if (lastUserId != null || _channels.value.isNotEmpty()) return
        lastUserId = saved.userId
        AppLog.i(FOLLOWED_CHANNELS_TAG, "showing ${saved.channels.size} saved at start")
        _fromCache.value = true
        _channels.value = saved.channels
        _status.value = FollowedChannelsStatus.Ready
    }

    private suspend fun showCached(userId: String) {
        val cached = withContext(Dispatchers.IO) { cache.read(userId) }
        if (cached.isEmpty() || _channels.value.isNotEmpty()) return
        _fromCache.value = true
        _channels.value = cached
        _status.value = FollowedChannelsStatus.Ready
    }

    private suspend fun applyResult(result: FollowedChannelsResult, accessToken: String, userId: String) {
        when (result) {
            is FollowedChannelsResult.Ready -> {
                AppLog.i(
                    FOLLOWED_CHANNELS_TAG,
                    "loaded ${result.channels.size}, ${result.channels.count { it.isLive }} live",
                )
                val channels =
                    followedWithPreviews(_channels.value, result.channels, System.currentTimeMillis())
                prefetchFollowedPreviews(application, channels)
                _fromCache.value = false
                _channels.value = channels
                _status.value = FollowedChannelsStatus.Ready
                withContext(Dispatchers.IO) { cache.write(userId, channels) }
                if (result.liveKnown) StreamStartAlertsController.onFollowedLive(result.channels, accessToken)
            }
            FollowedChannelsResult.NeedsPermission -> {
                AppLog.i(FOLLOWED_CHANNELS_TAG, "load needs permission")
                _fromCache.value = false
                _channels.value = emptyList()
                _status.value = FollowedChannelsStatus.NeedsPermission
                withContext(Dispatchers.IO) { cache.clear() }
            }
            FollowedChannelsResult.Rejected -> {
                AppLog.i(FOLLOWED_CHANNELS_TAG, "load rejected")
                _status.value = FollowedChannelsStatus.Rejected
            }
            FollowedChannelsResult.Unavailable -> {
                AppLog.i(FOLLOWED_CHANNELS_TAG, "load unavailable")
                if (_channels.value.isEmpty()) {
                    _status.value = FollowedChannelsStatus.Unavailable
                } else {
                    _status.value = FollowedChannelsStatus.Ready
                }
            }
        }
    }
}

internal enum class FollowedChannelsStatus {
    Loading,
    Ready,
    NeedsPermission,
    Rejected,
    Unavailable,
}
