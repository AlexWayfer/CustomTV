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

    /** Shows the cached emotes of [channel] at once, then loads the enabled sources. */
    fun open(channel: String) {
        this.channel = channel
        val cachedId = ChannelAvatarRepository.cached(channel)?.id
        _sevenTv.value = SevenTvRepository.cached(cachedId).takeIf { sources.sevenTv }.orEmpty()
        _ffz.value = FfzRepository.cached(cachedId).takeIf { sources.ffz }.orEmpty()
        _bttv.value = BttvRepository.cached(cachedId).takeIf { sources.bttv }.orEmpty()
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
        sevenTvLoaded = SevenTvRepository.isLoaded(channelId),
        ffzLoaded = FfzRepository.isLoaded(channelId),
        bttvLoaded = BttvRepository.isLoaded(channelId),
    )

    private fun load(channel: String, forceRefresh: Boolean) {
        job?.cancel()
        job = scope.launch { loadRounds(channel, forceRefresh) }
    }

    private suspend fun loadRounds(channel: String, forceRefresh: Boolean) {
        var backoff = 2.seconds
        repeat(LOAD_ROUNDS) { round ->
            if (!isOpen(channel)) return
            val force = forceRefresh && round == 0
            val cachedId = ChannelAvatarRepository.cached(channel)?.id
            val channelId = supervisorScope {
                val profile = async {
                    if (cachedId != null && !force && round == 0) {
                        cachedId
                    } else {
                        resultUnlessCancelled { ChannelAvatarRepository.refresh(channel) }.getOrNull()?.id
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
            if (!isOpen(channel)) return
            if (ready(channelId)) return
            if (round == LOAD_ROUNDS - 1) return
            delay(backoff)
            backoff *= 2
        }
    }

    private suspend fun apply(channel: String, channelId: String?, forceRefresh: Boolean) {
        val enabled = sources
        supervisorScope {
            if (enabled.sevenTv) {
                launch {
                    resultUnlessCancelled { SevenTvRepository.restore(channelId) }.getOrNull()
                        ?.let { if (isOpen(channel)) _sevenTv.value = it }
                    val emotes = resultUnlessCancelled {
                        SevenTvRepository.forTwitchUser(channelId, forceRefresh)
                    }.getOrNull() ?: return@launch
                    if (!isOpen(channel)) return@launch
                    if (emoteLoadPublishes(emotes.size, SevenTvRepository.isLoaded(channelId))) {
                        _sevenTv.value = emotes
                    }
                }
            }
            if (enabled.ffz) {
                launch {
                    resultUnlessCancelled { FfzRepository.restore(channelId) }.getOrNull()
                        ?.let { if (isOpen(channel)) _ffz.value = it }
                    val emotes = resultUnlessCancelled {
                        FfzRepository.forTwitchUser(channelId, forceRefresh)
                    }.getOrNull() ?: return@launch
                    if (!isOpen(channel)) return@launch
                    if (emoteLoadPublishes(emotes.size, FfzRepository.isLoaded(channelId))) {
                        _ffz.value = emotes
                    }
                }
            }
            if (enabled.bttv) {
                launch {
                    resultUnlessCancelled { BttvRepository.restore(channelId) }.getOrNull()
                        ?.let { if (isOpen(channel)) _bttv.value = it }
                    val emotes = resultUnlessCancelled {
                        BttvRepository.forTwitchUser(channelId, forceRefresh)
                    }.getOrNull() ?: return@launch
                    if (!isOpen(channel)) return@launch
                    if (emoteLoadPublishes(emotes.size, BttvRepository.isLoaded(channelId))) {
                        _bttv.value = emotes
                    }
                }
            }
        }
        if (isOpen(channel)) onChannelResolved(channelId)
    }

    private fun isOpen(channel: String): Boolean = channelResultApplies(channel, this.channel)

    /** Applies a 7TV or BTTV emote change from the live event socket. Null hides the event line. */
    fun applyLiveChange(channelId: String, message: ChatMessage, change: ChatEmoteChange): ChatMessage? =
        when (change.platform) {
            EmotePlatform.SevenTv -> applySevenTvChange(channelId, message, change)
            EmotePlatform.Bttv -> applyBttvChange(channelId, message, change)
            EmotePlatform.Ffz -> message
        }

    private fun applySevenTvChange(
        channelId: String,
        message: ChatMessage,
        change: ChatEmoteChange,
    ): ChatMessage {
        val maps = when (change.action) {
            EmoteChangeAction.Added -> {
                val emote = change.emote ?: return message
                SevenTvRepository.addChannelEmote(channelId, change.emoteName, emote)
            }
            EmoteChangeAction.Removed -> {
                SevenTvRepository.removeChannelEmote(channelId, change.emoteName)
            }
            EmoteChangeAction.Renamed -> {
                val oldName = change.previousName ?: return message
                val emote = change.emote ?: return message
                SevenTvRepository.renameChannelEmote(channelId, oldName, change.emoteName, emote)
            }
        }
        maps?.let { _sevenTv.value = it }
        return message
    }

    private fun applyBttvChange(
        channelId: String,
        message: ChatMessage,
        change: ChatEmoteChange,
    ): ChatMessage? {
        return when (change.action) {
            EmoteChangeAction.Added -> {
                val emote = change.emote ?: return message
                BttvRepository.addChannelEmote(channelId, change.emoteName, emote)
                    ?.let { _bttv.value = it }
                message
            }
            EmoteChangeAction.Removed -> {
                val resolvedName = change.emoteName.ifBlank {
                    change.emoteId
                        ?.let { BttvRepository.channelEmoteNameById(channelId, it) }
                        .orEmpty()
                }
                val cachedEmote = resolvedName.takeIf { it.isNotBlank() }
                    ?.let { BttvRepository.channelEmote(channelId, it) }
                val maps = if (resolvedName.isNotBlank()) {
                    BttvRepository.removeChannelEmote(channelId, resolvedName)
                } else {
                    change.emoteId?.let { BttvRepository.removeChannelEmoteById(channelId, it)?.second }
                }
                maps?.let { _bttv.value = it }
                resolveBttvRemovalMessage(message, resolvedName, cachedEmote)
            }
            EmoteChangeAction.Renamed -> {
                val emote = change.emote ?: return message
                val oldName = change.previousName
                    ?: change.emoteId?.let { BttvRepository.channelEmoteNameById(channelId, it) }
                if (oldName != null && oldName == change.emoteName) {
                    BttvRepository.addChannelEmote(channelId, change.emoteName, emote)
                        ?.let { _bttv.value = it }
                    return null
                }
                val maps = if (oldName != null) {
                    BttvRepository.renameChannelEmote(channelId, oldName, change.emoteName, emote)
                } else {
                    BttvRepository.addChannelEmote(channelId, change.emoteName, emote)
                }
                maps?.let { _bttv.value = it }
                resolveBttvRenameMessage(message, oldName)
            }
        }
    }

    private companion object {
        const val LOAD_ROUNDS = 3
    }
}
