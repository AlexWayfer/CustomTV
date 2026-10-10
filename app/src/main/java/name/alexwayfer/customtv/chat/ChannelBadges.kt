package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChatBadgeRepository

/** Badge image URLs for the open channel: global badges plus the channel's own. */
internal class ChannelBadges(private val scope: CoroutineScope) {
    private val _urls = MutableStateFlow<Map<String, String>>(emptyMap())
    val urls: StateFlow<Map<String, String>> = _urls
    private var channel: String? = null
    private var job: Job? = null

    fun open(channel: String) {
        this.channel = channel
        job?.cancel()
        _urls.value = emptyMap()
        job = scope.launch {
            resultUnlessCancelled { ChatBadgeRepository.restore(channel) }.getOrNull()?.let { stored ->
                if (channelResultApplies(channel, this@ChannelBadges.channel)) _urls.value = stored
            }
            val loaded = resultUnlessCancelled { ChatBadgeRepository.forChannel(channel) }.getOrNull()
                ?: return@launch
            if (channelResultApplies(channel, this@ChannelBadges.channel)) _urls.value = loaded
        }
    }
}
