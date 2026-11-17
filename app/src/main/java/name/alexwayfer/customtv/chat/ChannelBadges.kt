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
internal class ChannelBadges(
    private val scope: CoroutineScope,
    private val restore: suspend (String) -> ChannelBadgeUrls? = ChatBadgeRepository::restore,
    private val load: suspend (String, (ChannelBadgeUrls) -> Unit) -> Unit = ChatBadgeRepository::keepLoaded,
) {
    private val loaded = MutableStateFlow(ChannelBadgeUrls())
    val urls: StateFlow<Map<String, String>> =
        combine(loaded, AppSettingsStore.restored) { badges, settings ->
            badges.shown(ffzBadges = (settings ?: AppSettings()).ffzBadges)
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    private var channel: String? = null
    private var job: Job? = null
    private var foreground = true

    fun setForeground(foreground: Boolean) {
        if (this.foreground == foreground) return
        this.foreground = foreground
        job?.cancel()
        job = null
        if (foreground) channel?.let(::start)
    }

    fun open(channel: String) {
        this.channel = channel
        job?.cancel()
        loaded.value = ChannelBadgeUrls()
        if (foreground) start(channel)
    }

    private fun start(channel: String) {
        job = scope.launch {
            loadResult { restore(channel) }.getOrNull()?.let { stored ->
                publish(channel, stored)
            }
            load(channel) { fetched ->
                publish(channel, fetched)
            }
        }
    }

    private fun publish(channel: String, badges: ChannelBadgeUrls) {
        if (foreground && channelResultApplies(channel, this.channel)) loaded.value = badges
    }
}
