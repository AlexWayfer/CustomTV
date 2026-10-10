package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.CheermoteRepository
import name.alexwayfer.customtv.data.CheermoteTier

/** Bits cheer images for the open channel. The cached set shows at once; the network load replaces it. */
internal class ChannelCheermotes(private val scope: CoroutineScope) {
    private val _tiers = MutableStateFlow<Map<String, List<CheermoteTier>>>(emptyMap())
    val tiers: StateFlow<Map<String, List<CheermoteTier>>> = _tiers
    private var channel: String? = null
    private var job: Job? = null

    /** [onLoaded] runs once the network set replaces the cached one, so shown messages can use it. */
    fun open(channel: String, onLoaded: (Map<String, List<CheermoteTier>>) -> Unit = {}) {
        this.channel = channel
        job?.cancel()
        _tiers.value = CheermoteRepository.cached(channel) ?: emptyMap()
        job = scope.launch {
            resultUnlessCancelled { CheermoteRepository.restore(channel) }.getOrNull()?.let { stored ->
                if (channelResultApplies(channel, this@ChannelCheermotes.channel)) _tiers.value = stored
            }
            val loaded = resultUnlessCancelled { CheermoteRepository.forChannel(channel) }.getOrNull()
                ?: return@launch
            if (!channelResultApplies(channel, this@ChannelCheermotes.channel)) return@launch
            _tiers.value = loaded
            onLoaded(loaded)
        }
    }
}
