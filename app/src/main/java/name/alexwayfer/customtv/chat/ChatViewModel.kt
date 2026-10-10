package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.AppSettingsStore
import name.alexwayfer.customtv.data.ChannelAvatarRepository
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.PinnedChatRepository
import name.alexwayfer.customtv.data.SevenTvRepository
import name.alexwayfer.customtv.diagnostics.AppLog
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ChatViewModel(
    private val client: TwitchIrcClient = TwitchIrcClient(),
    private val pubSub: TwitchPubSubClient = TwitchPubSubClient(),
    private val sevenTvEvents: SevenTvEventClient = SevenTvEventClient(),
    private val bttvEvents: BttvEventClient = BttvEventClient(),
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _recentChatLoadState = MutableStateFlow(RecentChatLoadState.Idle)
    internal val recentChatLoadState: StateFlow<RecentChatLoadState> = _recentChatLoadState
    private val _recentAuthorMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val recentAuthorMessages: StateFlow<Map<String, List<ChatMessage>>> = _recentAuthorMessages

    /** Messages as they arrive in live chat, without recent chat history. */
    internal val liveIrcMessages: SharedFlow<ChatMessage> = client.messages
    val connectionState: StateFlow<ChatConnectionState> = client.state
    internal val roomModes: StateFlow<ChatRoomModes> = client.roomModes

    private val badges = ChannelBadges(viewModelScope)
    val badgeUrls: StateFlow<Map<String, String>> = badges.urls

    private val emotes = ChannelEmotes(viewModelScope, onChannelResolved = ::listenEmoteEvents)
    val sevenTvEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.sevenTv
    val ffzEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.ffz
    val bttvEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.bttv

    private val cheermotes = ChannelCheermotes(viewModelScope)

    private val _nickColors = MutableStateFlow<Map<String, Color>>(emptyMap())
    val nickColors: StateFlow<Map<String, Color>> = _nickColors

    private val _pinnedChat = MutableStateFlow<PinnedChat?>(null)
    val pinnedChat: StateFlow<PinnedChat?> = _pinnedChat
    private val _outgoingRaid = MutableStateFlow<OutgoingRaid?>(null)
    internal val outgoingRaid: StateFlow<OutgoingRaid?> = _outgoingRaid
    private val liveEvents = ChannelEventsController(viewModelScope)
    internal val channelEvents: StateFlow<ChannelEvents> = liveEvents.events

    private var collectJob: Job? = null
    private var reconnectJob: Job? = null
    private var pinExpireJob: Job? = null
    private var currentChannel: String? = null
    private var currentChannelId: String? = null
    private var raidToken: String? = null
    private var currentPin: PinnedChat? = null
    private var hiddenPinId: String? = null
    private var pinGeneration = 0
    private var hasWelcomed = false
    private var loadRecentChatOnConnect = false
    private var recentChatLoadStarted = false
    private var inForeground = true
    private var pendingRewardEvents: List<ChatMessage> = emptyList()
    private val _sessionChatters = MutableStateFlow<Map<String, String>>(emptyMap())
    internal val sessionChatters: StateFlow<Map<String, String>> = _sessionChatters

    init {
        collectJob = viewModelScope.launch {
            merge(
                client.messages,
                pubSub.messages,
                sevenTvEvents.messages,
                bttvEvents.messages,
            ).collect { message ->
                when (message.eventKind) {
                    ChatEventKind.MessageDeleted -> markMessageDeleted(message)
                    ChatEventKind.UserMessagesDeleted -> markUserMessagesDeleted(message)
                    else -> {
                        val enriched = applyLiveEmoteChange(
                            CheermoteMatcher.apply(enrichReward(message), cheermotes.tiers.value),
                        ) ?: return@collect
                        val seen = rememberSessionChatter(_sessionChatters.value, enriched)
                        if (seen.chatters !== _sessionChatters.value) {
                            _sessionChatters.value = seen.chatters
                        }
                        val chatterMessage = if (seen.firstInSession) {
                            enriched.copy(firstInSession = true)
                        } else {
                            enriched
                        }
                        var previousMessages = emptyList<ChatMessage>()
                        var updatedMessages = emptyList<ChatMessage>()
                        _messages.update { current ->
                            previousMessages = current
                            val result = RewardMessageDeduper.apply(
                                current,
                                paintKnownRewardColor(chatterMessage),
                                MAX_CHAT_MESSAGES,
                                pendingRewardEvents,
                            )
                            pendingRewardEvents = result.pending
                            result.colorSource?.let { rememberParticipantColor(it) }
                            result.messages.also { updatedMessages = it }
                        }
                        _recentAuthorMessages.update { history ->
                            rememberRecentAuthorMessages(history, previousMessages, updatedMessages)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var previous: ChatConnectionState? = null
            client.state.collect { state ->
                handleConnectionState(previous, state)
                previous = state
                if (state == ChatConnectionState.Reconnecting) {
                    scheduleReconnect()
                }
            }
        }
        viewModelScope.launch {
            pubSub.pins.collect { handlePinnedUpdate(it) }
        }
        viewModelScope.launch { pubSub.polls.collect(liveEvents::onPoll) }
        viewModelScope.launch { pubSub.predictions.collect(liveEvents::onPrediction) }
        viewModelScope.launch { pubSub.hypeTrains.collect(liveEvents::onHypeTrain) }
        viewModelScope.launch {
            AppSettingsStore.restored
                .map(::emoteSources)
                .distinctUntilChanged()
                .collect(emotes::setSources)
        }
        viewModelScope.launch {
            pubSub.raids.collect { event ->
                val current = _outgoingRaid.value
                val createdId = outgoingRaidCreatedId(current, event)
                _outgoingRaid.value = applyRaidEvent(current, event)
                if (createdId != null) appendRaidCreated(createdId)
            }
        }
        viewModelScope.launch {
            pubSub.opened.collect { channelId ->
                if (channelId != currentChannelId || _outgoingRaid.value == null) return@collect
                val lookup = OutgoingRaidLookup.load(channelId)
                if (channelId != currentChannelId) return@collect
                _outgoingRaid.value = recheckOutgoingRaid(_outgoingRaid.value, lookup, System.currentTimeMillis())
                AppLog.i("Raid", "raid recheck after reconnect: ${lookup.javaClass.simpleName}")
            }
        }
    }

    internal fun setRaidToken(token: String?) {
        val normalized = token?.takeIf { it.isNotBlank() }
        if (raidToken == normalized) return
        raidToken = normalized
        currentChannelId?.let { pubSub.listen(it, normalized) }
    }

    internal fun hideChannelEvent(id: String) = liveEvents.hide(id)

    /** Rows another live source adds or changes, such as messages held for moderators. */
    internal fun editMessages(transform: (List<ChatMessage>) -> List<ChatMessage>) {
        _messages.update { current -> transform(current).takeLast(MAX_CHAT_MESSAGES) }
    }

    internal fun dismissOutgoingRaid() {
        _outgoingRaid.value = null
    }

    private fun appendRaidCreated(raidId: String) {
        val message = ChatMessage(
            id = "raid-created-$raidId",
            userLogin = "",
            displayName = "",
            color = Color.Unspecified,
            rawText = "",
            parts = emptyList(),
            timestampMillis = System.currentTimeMillis(),
            eventKind = ChatEventKind.Raid,
            raid = ChatRaid(viewerCount = 0, fromDisplayName = "", created = true),
        )
        _messages.update { current ->
            if (current.any { it.id == message.id }) current
            else (current + message).takeLast(MAX_CHAT_MESSAGES)
        }
    }

    fun connect(channel: String, loadRecentChat: Boolean) {
        val normalized = channel.lowercase().removePrefix("#")
        if (normalized == currentChannel &&
            connectionState.value == ChatConnectionState.Connected
        ) {
            revealHiddenPin()
            return
        }
        hiddenPinId = null
        if (normalized != currentChannel) {
            _recentAuthorMessages.value = emptyMap()
            sevenTvEvents.disconnect()
            bttvEvents.disconnect()
            pubSub.disconnect()
            currentChannelId = null
            _outgoingRaid.value = null
            pinGeneration++
            applyPinned(null)
            liveEvents.clear()
        } else {
            publishPinned()
        }
        currentChannel = normalized
        reconnectJob?.cancel()
        hasWelcomed = false
        pendingRewardEvents = emptyList()
        _sessionChatters.value = emptyMap()
        _messages.value = listOf(chatNoticeMessage(ChatNotice.Connecting, CHAT_WELCOME_NOTICE_ID))
        _recentChatLoadState.value = RecentChatLoadState.Idle
        loadRecentChatOnConnect = loadRecentChat
        recentChatLoadStarted = false
        _nickColors.value = emptyMap()
        val cachedId = ChannelAvatarRepository.cached(normalized)?.id
        currentChannelId = cachedId
        client.connect(normalized)
        cachedId?.let {
            pubSub.listen(it, raidToken)
            listenEmoteEvents(it)
            loadPinned(it)
            liveEvents.load(it)
        }
        viewModelScope.launch {
            val lookup = ChannelAvatarRepository.lookup(normalized)
            val channelId = (lookup as? ChannelLookup.Found)?.profile?.id
            if (channelId != null && currentChannel == normalized) {
                currentChannelId = channelId
                pubSub.listen(channelId, raidToken)
                listenEmoteEvents(channelId)
                loadPinned(channelId)
                liveEvents.load(channelId)
            } else if (currentChannel == normalized) {
                channelLookupNotice(lookup)?.let { notice ->
                    _messages.update { current ->
                        insertRecentChatBeforeWelcome(
                            messages = current,
                            recent = listOf(chatNoticeMessage(notice, "channel-not-found-${System.nanoTime()}")),
                            initialNoticeId = CHAT_WELCOME_NOTICE_ID,
                            maxMessages = MAX_CHAT_MESSAGES,
                        )
                    }
                }
            }
        }
        badges.open(normalized)
        cheermotes.open(normalized) { loaded ->
            _messages.update { messages ->
                messages.map { CheermoteMatcher.apply(it, loaded) }
            }
            _recentAuthorMessages.update { history ->
                history.mapValues { (_, messages) ->
                    messages.map { CheermoteMatcher.apply(it, loaded) }
                }
            }
        }
        emotes.open(normalized)
    }

    fun refreshEmotes(): Job = emotes.refresh()

    fun setForeground(foreground: Boolean) {
        if (inForeground == foreground) return
        inForeground = foreground
        reconnectJob?.cancel()
        if (!foreground) {
            client.pause()
            pubSub.disconnect()
            sevenTvEvents.disconnect()
            bttvEvents.disconnect()
            return
        }
        val channel = currentChannel ?: return
        client.connect(channel)
        ChannelAvatarRepository.cached(channel)?.id?.let { channelId ->
            currentChannelId = channelId
            pubSub.listen(channelId, raidToken)
            listenEmoteEvents(channelId)
            loadPinned(channelId)
            liveEvents.load(channelId)
        }
        emotes.loadIfIncomplete(ChannelAvatarRepository.cached(channel)?.id)
    }

    private fun listenEmoteEvents(channelId: String?) {
        if (channelId.isNullOrBlank() || !inForeground) return
        currentChannelId = channelId
        SevenTvRepository.channelEmoteSetId(channelId)?.let { sevenTvEvents.listen(it) }
        bttvEvents.listen(channelId)
    }

    private fun paintKnownRewardColor(message: ChatMessage): ChatMessage {
        if (message.eventKind != ChatEventKind.Reward) return message
        if (!RewardMessageDeduper.isPubSub(message)) return message
        val known = knownChatColor(message.displayName, message.userLogin) ?: return message
        return message.copy(color = known)
    }

    private fun rememberParticipantColor(message: ChatMessage) {
        _nickColors.update { current ->
            rememberParticipantColor(current, message, MAX_NICK_COLORS)
        }
    }

    private fun applyLiveEmoteChange(message: ChatMessage): ChatMessage? {
        message.emoteChange ?: return message
        val colored = resolveEmoteActorColor(message)
        val channelId = currentChannelId ?: return colored
        return emotes.applyLiveChange(channelId, colored, colored.emoteChange!!)
    }

    private fun resolveEmoteActorColor(message: ChatMessage): ChatMessage {
        val change = message.emoteChange ?: return message
        return resolveEmoteActorColor(
            message,
            knownChatColor(change.actorName, message.userLogin),
        )
    }

    private fun knownChatColor(actorName: String, login: String): Color? {
        return knownParticipantColor(_nickColors.value, actorName, login)
    }


    private fun markMessageDeleted(event: ChatMessage) {
        _messages.update { current ->
            markMessageDeleted(current, event.id, event.deletedBy)
        }
        _recentAuthorMessages.update { history ->
            markRecentAuthorMessageDeleted(history, event.id, event.deletedBy)
        }
    }

    private fun markUserMessagesDeleted(event: ChatMessage) {
        _messages.update { current ->
            val marked = markUserMessagesDeleted(
                current,
                event.userLogin,
                event.deletedBy,
                event.timeoutSeconds,
                event.banned,
            )
            withModerationNotice(marked, clearChatNoticeRow(event)).takeLast(MAX_CHAT_MESSAGES)
        }
        _recentAuthorMessages.update { history ->
            markRecentAuthorMessagesDeleted(
                history,
                event.userLogin,
                event.deletedBy,
                event.timeoutSeconds,
                event.banned,
            )
        }
    }

    private fun enrichReward(message: ChatMessage): ChatMessage {
        val reward = message.reward ?: return message
        val channel = currentChannel ?: return message
        return mergeKnownReward(message, ChannelAvatarRepository.customReward(channel, reward.id))
    }

    private fun handleConnectionState(
        previous: ChatConnectionState?,
        state: ChatConnectionState,
    ) {
        when (state) {
            ChatConnectionState.Connecting -> {
                if (!inForeground) return
                if (!hasWelcomed) {
                    _messages.value = listOf(
                        chatNoticeMessage(ChatNotice.Connecting, CHAT_WELCOME_NOTICE_ID),
                    )
                } else if (previous == ChatConnectionState.Disconnected) {
                    showReconnectNotice(ChatNotice.Reconnecting)
                }
            }
            ChatConnectionState.Connected -> {
                if (!hasWelcomed) {
                    hasWelcomed = true
                    showWelcomeNotice()
                    startRecentChatLoad()
                } else if (previous != ChatConnectionState.Connected) {
                    showReconnectNotice(ChatNotice.Connected)
                }
            }
            ChatConnectionState.Reconnecting -> {
                if (!inForeground) return
                if (!hasWelcomed) return
                if (previous == ChatConnectionState.Connected) {
                    appendDisconnectedNotice()
                }
                showReconnectNotice(ChatNotice.Reconnecting)
            }
            ChatConnectionState.Disconnected -> {
                if (!inForeground) return
                if (hasWelcomed && previous == ChatConnectionState.Connected) {
                    appendDisconnectedNotice()
                }
            }
        }
    }

    private fun appendDisconnectedNotice() {
        _messages.update { current ->
            (current + chatNoticeMessage(ChatNotice.Disconnected, newStatusNoticeId()))
                .takeLast(MAX_CHAT_MESSAGES)
        }
    }

    private fun showReconnectNotice(notice: ChatNotice) {
        _messages.update { current ->
            replaceOrAppendReconnectNotice(
                messages = current,
                notice = notice,
                newId = newStatusNoticeId(),
                maxMessages = MAX_CHAT_MESSAGES,
            )
        }
    }

    private fun newStatusNoticeId(): String = "system-${System.nanoTime()}"

    private fun showWelcomeNotice() {
        _messages.update { current ->
            replaceOrAppendWelcomeNotice(current, CHAT_WELCOME_NOTICE_ID, MAX_CHAT_MESSAGES)
        }
    }

    private fun startRecentChatLoad() {
        val channel = currentChannel ?: return
        if (!loadRecentChatOnConnect || recentChatLoadStarted) return
        recentChatLoadStarted = true
        _recentChatLoadState.value = RecentChatLoadState.Loading
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = loadRecentChatMessages(channel)
                if (currentChannel == channel) {
                    val failure = recentChatNotice(result)?.let { notice ->
                        chatNoticeMessage(notice, "recent-chat-failed-${System.nanoTime()}")
                    }
                    var merge: RecentChatSessionMerge? = null
                    _messages.update { current ->
                        val next = mergeRecentChatSession(result.messages, current, _sessionChatters.value)
                        merge = next
                        insertRecentChatBeforeWelcome(next.current, next.recent + listOfNotNull(failure), CHAT_WELCOME_NOTICE_ID, MAX_CHAT_MESSAGES)
                    }
                    merge?.let { applied ->
                        _sessionChatters.value = applied.chatters
                        _recentAuthorMessages.update { history ->
                            prependRecentAuthorMessages(history, applied.recent, applied.movedFirstLogins)
                        }
                    }
                }
            } finally {
                if (currentChannel == channel) _recentChatLoadState.value = RecentChatLoadState.Idle
            }
        }
    }

    private fun scheduleReconnect() {
        if (!inForeground) return
        val channel = currentChannel ?: return
        reconnectJob?.cancel()
        AppLog.i(TAG, "retry in ${RECONNECT_DELAY.inWholeMilliseconds}ms")
        reconnectJob = viewModelScope.launch {
            delay(RECONNECT_DELAY)
            if (currentChannel == channel) {
                client.connect(channel)
            }
        }
    }

    fun hidePinnedForSelf() {
        val pin = currentPin ?: return
        hiddenPinId = pin.pinId
        _pinnedChat.value = null
    }

    private fun revealHiddenPin() {
        if (hiddenPinId == null) return
        hiddenPinId = null
        _pinnedChat.value = currentPin
    }

    private fun handlePinnedUpdate(update: PinnedChatUpdate) {
        when (update) {
            is PinnedChatUpdate.Set -> {
                pinGeneration++
                applyPinned(update.pin.copy(live = true))
            }
            is PinnedChatUpdate.Clear -> {
                val current = currentPin
                if (!shouldClearPinnedChat(current, update.pinId)) return
                pinGeneration++
                applyPinned(null)
            }
            is PinnedChatUpdate.Duration -> {
                val current = currentPin
                if (current == null) {
                    pinGeneration++
                    currentChannelId?.let(::loadPinned)
                    return
                }
                val updated = updatePinnedChatDuration(current, update.pinId, update.endsAtMillis)
                if (updated === current) return
                pinGeneration++
                applyPinned(updated)
            }
            PinnedChatUpdate.Refresh -> {
                pinGeneration++
                currentChannelId?.let(::loadPinned)
            }
        }
    }

    private fun loadPinned(channelId: String) {
        val generation = pinGeneration
        viewModelScope.launch {
            val pin = resultUnlessCancelled { PinnedChatRepository.fetch(channelId) }
                .getOrElse { return@launch }
            if (generation != pinGeneration || currentChannelId != channelId) return@launch
            applyPinned(pin)
        }
    }

    private fun applyPinned(pin: PinnedChat?) {
        pinExpireJob?.cancel()
        currentPin = pin
        if (pin != null && pin.pinId != hiddenPinId) {
            hiddenPinId = null
        }
        publishPinned()
        pin?.message?.let(::rememberParticipantColor)
        val endsAt = pin?.endsAtMillis ?: return
        val wait = endsAt - System.currentTimeMillis()
        if (wait <= 0L) {
            currentPin = null
            publishPinned()
            return
        }
        val pinId = pin.pinId
        pinExpireJob = viewModelScope.launch {
            delay(wait.milliseconds)
            if (currentPin?.pinId == pinId) {
                currentPin = null
                publishPinned()
            }
        }
    }

    private fun publishPinned() {
        _pinnedChat.value = visiblePinnedChat(currentPin, hiddenPinId)
    }

    override fun onCleared() {
        reconnectJob?.cancel()
        collectJob?.cancel()
        pinExpireJob?.cancel()
        sevenTvEvents.close()
        bttvEvents.close()
        pubSub.close()
        client.disconnect()
    }

    private companion object {
        const val MAX_NICK_COLORS = 800
        const val TAG = "ChatIrc"
        val RECONNECT_DELAY = 1.seconds
    }
}
