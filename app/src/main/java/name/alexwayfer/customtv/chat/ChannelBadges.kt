package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettings
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.ChannelBadgeUrls
import name.alexwayfer.customtv.data.ChatBadgeRepository

/** Badge image URLs for the open channel: global badges plus the channel's own, and FFZ's while its switch is on. */
internal class ChannelBadges(private val scope: CoroutineScope) {
    private val loaded = MutableStateFlow(ChannelBadgeUrls())
    val urls: StateFlow<Map<String, String>> =
        combine(loaded, AppSettingsStore.restored) { badges, settings ->
            badges.shown(ffzBadges = (settings ?: AppSettings()).ffzBadges)
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    private var channel: String? = null
    private var job: Job? = null

    fun open(channel: String) {
        this.channel = channel
        job?.cancel()
        loaded.value = ChannelBadgeUrls()
        job = scope.launch {
            loadResult { ChatBadgeRepository.restore(channel) }.getOrNull()?.let { stored ->
                if (channelResultApplies(channel, this@ChannelBadges.channel)) loaded.value = stored
            }
            val fetched = loadResult { ChatBadgeRepository.forChannel(channel) }.getOrNull()
                ?: return@launch
            if (channelResultApplies(channel, this@ChannelBadges.channel)) loaded.value = fetched
        }
    }
}
