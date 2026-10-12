package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import name.alexwayfer.customtv.data.BttvRepository
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.EmoteSourceCache
import name.alexwayfer.customtv.data.FfzRepository
import name.alexwayfer.customtv.data.SevenTvRepository
import kotlin.time.Duration.Companion.seconds

/**
 * 7TV, FFZ, and BTTV emotes for the open channel. Only enabled sources load. Every result is
 * checked against the channel still open, so a slow load cannot replace a newer channel's emotes.
 * [onChannelResolved] reports the channel id once its emotes were applied, for live emote events.
 */
internal class ChannelEmotes(
    private val scope: CoroutineScope,
    private val onChannelResolved: (channelId: String?) -> Unit = {},
) {
    private val _sevenTv = MutableStateFlow<Map<String, SevenTvEmote>>(emptyMap())
    val sevenTv: StateFlow<Map<String, SevenTvEmote>> = _sevenTv
    private val _ffz = MutableStateFlow<Map<String, SevenTvEmote>>(emptyMap())
    val ffz: StateFlow<Map<String, SevenTvEmote>> = _ffz
    private val _bttv = MutableStateFlow<Map<String, SevenTvEmote>>(emptyMap())
    val bttv: StateFlow<Map<String, SevenTvEmote>> = _bttv

    private var channel: String? = null
    private var sources = emoteSources(null)
    private var job: Job? = null
    private val liveChanges = LiveEmoteChanges(_sevenTv, _bttv)

    /** Shows the cached emotes of [channel] at once, then loads the enabled sources. */
    fun open(channel: String) {
        this.channel = channel
        val cachedId = ChannelAvatarRepository.cached(channel)?.id
        _sevenTv.value = SevenTvRepository.emotes.cached(cachedId).takeIf { sources.sevenTv }.orEmpty()
        _ffz.value = FfzRepository.emotes.cached(cachedId).takeIf { sources.ffz }.orEmpty()
        _bttv.value = BttvRepository.emotes.cached(cachedId).takeIf { sources.bttv }.orEmpty()
        load(channel, forceRefresh = false)
    }

    fun setSources(next: EmoteSources) {
        val turnedOn = emoteSourceTurnedOn(sources, next)
        sources = next
        val open = channel ?: return
        if (turnedOn) load(open, forceRefresh = false)
    }

    fun refresh(): Job {
        val open = channel ?: return Job().apply { complete() }
        load(open, forceRefresh = true)
        return job ?: Job().apply { complete() }
    }

    /** Loads again when an enabled source is still missing, for example after the app was in the background. */
    fun loadIfIncomplete(channelId: String?) {
        val open = channel ?: return
        if (!ready(channelId)) load(open, forceRefresh = false)
    }

    private fun ready(channelId: String?): Boolean = emoteSourcesReady(
        channelId = channelId,
        sources = sources,
        sevenTvLoaded = SevenTvRepository.emotes.isLoaded(channelId),
        ffzLoaded = FfzRepository.emotes.isLoaded(channelId),
        bttvLoaded = BttvRepository.emotes.isLoaded(channelId),
    )

    private fun load(channel: String, forceRefresh: Boolean) {
        job?.cancel()
        job = scope.launch { loadRounds(channel, forceRefresh) }
    }

    private suspend fun loadRounds(channel: String, forceRefresh: Boolean) {
        var backoff = 2.seconds
        for (round in 0 until LOAD_ROUNDS) {
            if (!isOpen(channel) || loadRound(channel, round, forceRefresh)) break
            if (round < LOAD_ROUNDS - 1) {
                delay(backoff)
                backoff *= 2
            }
        }
    }

    /** One load of [channel]'s emotes; true when nothing is left to try again, or the channel closed. */
    private suspend fun loadRound(channel: String, round: Int, forceRefresh: Boolean): Boolean {
        val force = forceRefresh && round == 0
        val cachedId = ChannelAvatarRepository.cached(channel)?.id
        val channelId = supervisorScope {
            val profile = async {
                if (cachedId != null && !force && round == 0) {
                    cachedId
                } else {
                    loadResult { ChannelAvatarRepository.refresh(channel) }.getOrNull()?.id
                        ?: ChannelAvatarRepository.cached(channel)?.id
                        ?: cachedId
                }
            }
            if (cachedId == null && !force) {
                apply(channel, null, forceRefresh = false)
            }
            profile.await()
        }
        apply(channel, channelId, force)
        return !isOpen(channel) || ready(channelId)
    }

    private suspend fun apply(channel: String, channelId: String?, forceRefresh: Boolean) {
        val enabled = sources
        val open = { isOpen(channel) }
        supervisorScope {
            if (enabled.sevenTv) {
                launch { loadEmoteSource(SevenTvRepository.emotes, channelId, forceRefresh, open, _sevenTv) }
            }
            if (enabled.ffz) {
                launch { loadEmoteSource(FfzRepository.emotes, channelId, forceRefresh, open, _ffz) }
            }
            if (enabled.bttv) {
                launch { loadEmoteSource(BttvRepository.emotes, channelId, forceRefresh, open, _bttv) }
            }
        }
        if (isOpen(channel)) onChannelResolved(channelId)
    }

    private fun isOpen(channel: String): Boolean = channelResultApplies(channel, this.channel)

    /** Applies a 7TV or BTTV emote change from the live event socket. Null hides the event line. */
    fun applyLiveChange(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage? =
        liveChanges.apply(channelId, message, change)

    private companion object {
        const val LOAD_ROUNDS = 3
    }
}

/**
 * Shows what the last start kept of one source for the channel, then the network's answer once it is complete enough;
 * nothing lands in [target] after the channel closed.
 */
private suspend fun loadEmoteSource(
    cache: EmoteSourceCache,
    channelId: String?,
    forceRefresh: Boolean,
    isOpen: () -> Boolean,
    target: MutableStateFlow<Map<String, SevenTvEmote>>,
) {
    loadResult { cache.restore(channelId) }.getOrNull()?.let { if (isOpen()) target.value = it }
    val emotes = loadResult { cache.forTwitchUser(channelId, forceRefresh) }.getOrNull() ?: return
    if (!isOpen()) return
    if (emoteLoadPublishes(emotes.size, cache.isLoaded(channelId))) target.value = emotes
}
