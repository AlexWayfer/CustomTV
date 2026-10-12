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
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.ChannelSearchRepository
import name.alexwayfer.customtv.data.channelSearchQuery
import name.alexwayfer.customtv.data.RECENT_CHANNELS_TAG
import name.alexwayfer.customtv.data.RecentChannelsSnapshot
import name.alexwayfer.customtv.data.RecentStream
import name.alexwayfer.customtv.data.recentProfileForDisplay
import name.alexwayfer.customtv.data.recentStreamsAfterRefresh
import name.alexwayfer.customtv.data.RecentChannel
import name.alexwayfer.customtv.data.RecentChannelsStore
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = RecentChannelsStore(application)
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
    internal val follows = HomeFollows(application, viewModelScope)

    val recents: StateFlow<List<RecentChannel>> = RecentChannelsStore.restored
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecentChannelsStore.restored.value.orEmpty())

    /** Whether the saved recents are read, so an empty list means there are none rather than not read yet. */
    val recentsRestored: StateFlow<Boolean> = RecentChannelsStore.restored
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecentChannelsStore.restored.value != null)

    init {
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
                val trigger = if (automatic) "automatic" else "manual"
                AppLog.i(RECENT_CHANNELS_TAG, "refresh ${channels.size} channels, $trigger")
                _recentProfiles.value = channels.mapNotNull { channel ->
                    cachedRecentProfile(channel.login)?.let { channel.login to it }
                }.toMap()
                val ids = channels.map { it.id }
                val profiles = ChannelAvatarRepository.refreshByIds(ids)
                AppLog.i(RECENT_CHANNELS_TAG, "refreshed ${profiles.size} of ${ids.size}")
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
