package name.alexwayfer.customtv.ui.home

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.ChannelSearchRepository
import name.alexwayfer.customtv.data.channelSearchQuery
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.FollowedChannelsAtStart
import name.alexwayfer.customtv.data.FollowedChannelsCache
import name.alexwayfer.customtv.data.RecentChannelsSnapshot
import name.alexwayfer.customtv.data.RecentStream
import name.alexwayfer.customtv.data.recentProfileForDisplay
import name.alexwayfer.customtv.data.recentStreamsAfterRefresh
import name.alexwayfer.customtv.data.FollowedChannelsRepository
import name.alexwayfer.customtv.data.FollowedChannelsResult
import name.alexwayfer.customtv.data.RecentChannel
import name.alexwayfer.customtv.data.RecentChannelsStore
import name.alexwayfer.customtv.data.StreamStartAlertsController
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.ui.account.TwitchUserAccess
import kotlin.time.Duration.Companion.milliseconds

private const val FOLLOWS_LOG_TAG = "FollowedChannels"
private const val RECENTS_LOG_TAG = "RecentChannels"

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = RecentChannelsStore(application)
    private val followsRepository = FollowedChannelsRepository(BuildConfig.TWITCH_CLIENT_ID)
    private val followsCache = FollowedChannelsCache(application)
    private val recentsSnapshot = RecentChannelsSnapshot(application)
    private var recentStreams: Map<String, RecentStream> = emptyMap()
    private val recentStreamsRestored = CompletableDeferred<Unit>()
    private val _checkingChannel = MutableStateFlow(false)
    val checkingChannel: StateFlow<Boolean> = _checkingChannel
    private val _refreshingRecents = MutableStateFlow(false)
    val refreshingRecents: StateFlow<Boolean> = _refreshingRecents
    private val _recentProfiles = MutableStateFlow<Map<String, ChannelProfile>>(emptyMap())
    val recentProfiles: StateFlow<Map<String, ChannelProfile>> = _recentProfiles
    private val _channelSearch = MutableStateFlow<List<ChannelSearchHit>>(emptyList())
    val channelSearch: StateFlow<List<ChannelSearchHit>> = _channelSearch
    private var channelSearchJob: Job? = null
    private var refreshingRecentRequest = false
    private var lastRecentRefreshAtMillis: Long? = null
    private val _followedChannels = MutableStateFlow<List<FollowedChannel>>(emptyList())
    internal val followedChannels: StateFlow<List<FollowedChannel>> = _followedChannels
    private val _followedFromCache = MutableStateFlow(false)
    /** The shown follow list is the one saved by the last start, not a fresh load yet. */
    internal val followedFromCache: StateFlow<Boolean> = _followedFromCache
    private val _followedStatus = MutableStateFlow(FollowedChannelsStatus.Loading)
    internal val followedStatus: StateFlow<FollowedChannelsStatus> = _followedStatus
    private val _refreshingFollows = MutableStateFlow(false)
    internal val refreshingFollows: StateFlow<Boolean> = _refreshingFollows
    private var followsRequest = false
    private var followsJob: Job? = null
    private var lastFollowRefreshAtMillis: Long? = null
    private var lastFollowUserId: String? = null

    val recents: StateFlow<List<RecentChannel>> = RecentChannelsStore.restored
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecentChannelsStore.restored.value.orEmpty())

    /** Whether the saved recents are read, so an empty list means there are none rather than not read yet. */
    val recentsRestored: StateFlow<Boolean> = RecentChannelsStore.restored
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecentChannelsStore.restored.value != null)

    init {
        viewModelScope.launch { showFollowsSavedAtStart() }
        viewModelScope.launch {
            ChannelAvatarRepository.awaitRestored()
            recentStreams = withContext(Dispatchers.IO) { recentsSnapshot.read() }
            recentStreamsRestored.complete(Unit)
            store.recents.collect { channels ->
                _recentProfiles.value = recentProfilesWithCache(
                    _recentProfiles.value,
                    channels.map { it.login },
                    ::cachedRecentProfile,
                )
            }
        }
    }

    fun rememberCachedProfiles() {
        _recentProfiles.value = recentProfilesWithCache(
            _recentProfiles.value,
            recents.value.map { it.login },
            ::cachedRecentProfile,
        )
    }

    /** The cached profile, with the stream saved at the last load until this run loads the channel. */
    private fun cachedRecentProfile(login: String): ChannelProfile? =
        ChannelAvatarRepository.cached(login)?.let(::recentProfileWithStream)

    private fun recentProfileWithStream(profile: ChannelProfile): ChannelProfile =
        recentProfileForDisplay(profile, recentStreams, ChannelAvatarRepository::loadedThisRun)

    fun updateChannelSearch(raw: String) {
        channelSearchJob?.cancel()
        val query = channelSearchQuery(raw)
        if (query == null) {
            _channelSearch.value = emptyList()
            return
        }
        channelSearchJob = viewModelScope.launch {
            delay(300.milliseconds)
            _channelSearch.value = ChannelSearchRepository.suggest(query)
        }
    }

    /** Recents are keyed by channel ID, so an unknown channel waits for its profile first. */
    fun addRecent(login: String) {
        viewModelScope.launch {
            val profile = ChannelAvatarRepository.cached(login) ?: ChannelAvatarRepository.refresh(login)
            val id = profile?.id ?: return@launch
            store.add(RecentChannel(id, profile.login))
        }
    }

    fun removeRecent(channel: RecentChannel) {
        viewModelScope.launch {
            store.remove(channel.id)
            _recentProfiles.value -= channel.login
        }
    }

    fun refreshRecents(showProgress: Boolean = true, automatic: Boolean = false) {
        if (refreshingRecentRequest) return
        val now = SystemClock.elapsedRealtime()
        if (automatic && !shouldAutomaticallyRefreshRecents(lastRecentRefreshAtMillis, now)) return
        lastRecentRefreshAtMillis = now
        refreshingRecentRequest = true
        if (showProgress) _refreshingRecents.value = true
        viewModelScope.launch {
            try {
                // A load before the saved streams are read would save over them.
                recentStreamsRestored.await()
                val channels = recents.value
                AppLog.i(RECENTS_LOG_TAG, "refresh ${channels.size} channels, ${if (automatic) "automatic" else "manual"}")
                _recentProfiles.value = channels.mapNotNull { channel ->
                    cachedRecentProfile(channel.login)?.let { channel.login to it }
                }.toMap()
                val ids = channels.map { it.id }
                val profiles = ChannelAvatarRepository.refreshByIds(ids)
                AppLog.i(RECENTS_LOG_TAG, "refreshed ${profiles.size} of ${ids.size}")
                _recentProfiles.value = profiles.map(::recentProfileWithStream).associateBy { it.login }
                val streams =
                    recentStreamsAfterRefresh(recentStreams, ids, profiles, ChannelAvatarRepository::loadedThisRun)
                if (streams != recentStreams) {
                    recentStreams = streams
                    withContext(Dispatchers.IO) { recentsSnapshot.write(streams) }
                }
                store.updateLogins(profiles.mapNotNull { profile -> profile.id?.let { it to profile.login } }.toMap())
            } finally {
                refreshingRecentRequest = false
                _refreshingRecents.value = false
            }
        }
    }

    internal fun refreshFollows(
        accessToken: String,
        userId: String,
        liveOnly: Boolean,
        automatic: Boolean = false,
    ) {
        if (followedListBelongsToAnotherAccount(lastFollowUserId, userId)) forgetFollows()
        if (followsRequest) return
        val status = _followedStatus.value
        if (liveOnly && (status == FollowedChannelsStatus.NeedsPermission || status == FollowedChannelsStatus.Rejected)) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (
            automatic &&
            !followedAutomaticRefreshAllowed(lastFollowRefreshAtMillis, now, lastFollowUserId, userId)
        ) {
            return
        }
        lastFollowRefreshAtMillis = now
        lastFollowUserId = userId
        val current = _followedChannels.value
        followsRequest = true
        if (current.isEmpty() || !liveOnly) _refreshingFollows.value = true
        if (current.isEmpty()) _followedStatus.value = FollowedChannelsStatus.Loading
        AppLog.i(
            FOLLOWS_LOG_TAG,
            "load ${if (liveOnly && current.isNotEmpty()) "live" else "full"}, showing ${current.size}" +
                if (_followedFromCache.value) " saved" else "",
        )
        followsJob = viewModelScope.launch {
            try {
                if (current.isEmpty()) showCachedFollows(userId)
                val result = if (liveOnly && current.isNotEmpty()) {
                    withContext(Dispatchers.IO) { followsRepository.refreshLive(accessToken, current) }
                } else {
                    followsRepository.load(accessToken, userId) { channels ->
                        val next = followedListDuringReload(
                            _followedChannels.value,
                            channels,
                            reloadFinished = false,
                        )
                        if (next !== _followedChannels.value) {
                            _followedChannels.value = followedWithPreviews(
                                _followedChannels.value,
                                next,
                                System.currentTimeMillis(),
                            )
                            _followedStatus.value = FollowedChannelsStatus.Ready
                        }
                    }
                }
                applyFollows(result, accessToken, userId)
            } finally {
                if (followsJob === coroutineContext[Job]) {
                    followsRequest = false
                    _refreshingFollows.value = false
                }
            }
        }
    }

    /** Loads the whole list again after the user followed or unfollowed a channel in the app. */
    internal fun reloadFollowsAfterChange(access: suspend () -> TwitchUserAccess?) {
        viewModelScope.launch {
            val current = access() ?: return@launch
            refreshFollows(current.accessToken, current.userId, liveOnly = false)
        }
    }

    /** Drops the list of the previous account, and its load still in flight, before the new account loads its own. */
    private fun forgetFollows() {
        followsJob?.cancel()
        followsJob = null
        followsRequest = false
        _refreshingFollows.value = false
        _followedFromCache.value = false
        _followedChannels.value = emptyList()
        _followedStatus.value = FollowedChannelsStatus.Loading
        lastFollowRefreshAtMillis = null
        lastFollowUserId = null
    }

    internal fun markFollowsUnavailable() {
        if (_followedChannels.value.isEmpty()) {
            _followedStatus.value = FollowedChannelsStatus.Unavailable
        }
    }

    /** Opens Home with the list read at app start, unless a load has already begun. */
    private suspend fun showFollowsSavedAtStart() {
        val saved = FollowedChannelsAtStart.take() ?: return
        if (lastFollowUserId != null || _followedChannels.value.isNotEmpty()) return
        lastFollowUserId = saved.userId
        AppLog.i(FOLLOWS_LOG_TAG, "showing ${saved.channels.size} saved at start")
        _followedFromCache.value = true
        _followedChannels.value = saved.channels
        _followedStatus.value = FollowedChannelsStatus.Ready
    }

    private suspend fun showCachedFollows(userId: String) {
        val cached = withContext(Dispatchers.IO) { followsCache.read(userId) }
        if (cached.isEmpty() || _followedChannels.value.isNotEmpty()) return
        _followedFromCache.value = true
        _followedChannels.value = cached
        _followedStatus.value = FollowedChannelsStatus.Ready
    }

    private suspend fun applyFollows(result: FollowedChannelsResult, accessToken: String, userId: String) {
        when (result) {
            is FollowedChannelsResult.Ready -> {
                AppLog.i(
                    FOLLOWS_LOG_TAG,
                    "loaded ${result.channels.size}, ${result.channels.count { it.isLive }} live",
                )
                val channels = followedWithPreviews(_followedChannels.value, result.channels, System.currentTimeMillis())
                prefetchFollowedPreviews(getApplication(), channels)
                _followedFromCache.value = false
                _followedChannels.value = channels
                _followedStatus.value = FollowedChannelsStatus.Ready
                withContext(Dispatchers.IO) { followsCache.write(userId, channels) }
                if (result.liveKnown) StreamStartAlertsController.onFollowedLive(result.channels, accessToken)
            }
            FollowedChannelsResult.NeedsPermission -> {
                AppLog.i(FOLLOWS_LOG_TAG, "load needs permission")
                _followedFromCache.value = false
                _followedChannels.value = emptyList()
                _followedStatus.value = FollowedChannelsStatus.NeedsPermission
                withContext(Dispatchers.IO) { followsCache.clear() }
            }
            FollowedChannelsResult.Rejected -> {
                AppLog.i(FOLLOWS_LOG_TAG, "load rejected")
                _followedStatus.value = FollowedChannelsStatus.Rejected
            }
            FollowedChannelsResult.Unavailable -> {
                AppLog.i(FOLLOWS_LOG_TAG, "load unavailable")
                if (_followedChannels.value.isEmpty()) {
                    _followedStatus.value = FollowedChannelsStatus.Unavailable
                } else {
                    _followedStatus.value = FollowedChannelsStatus.Ready
                }
            }
        }
    }

    fun openTypedChannel(login: String, onResult: (ChannelLookup) -> Unit) {
        if (_checkingChannel.value) return
        viewModelScope.launch {
            _checkingChannel.value = true
            try {
                val result = ChannelAvatarRepository.lookup(login)
                if (result is ChannelLookup.Found) {
                    _recentProfiles.value += result.profile.login to result.profile
                }
                onResult(result)
            } finally {
                _checkingChannel.value = false
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
