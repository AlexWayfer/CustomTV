package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
import name.alexwayfer.customtv.data.ChannelCustomRewards
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.SevenTvRepository

class ChatViewModel(
    private val client: TwitchIrcClient = TwitchIrcClient(),
    private val pubSub: TwitchPubSubClient = TwitchPubSubClient(),
    private val sevenTvEvents: SevenTvEventClient = SevenTvEventClient(),
    private val bttvEvents: BttvEventClient = BttvEventClient(),
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _chatterHistory = MutableStateFlow(ChatterHistory())
    internal val chatterHistory: StateFlow<ChatterHistory> = _chatterHistory
    private val _sessionChatters = MutableStateFlow<Map<String, String>>(emptyMap())
    internal val sessionChatters: StateFlow<Map<String, String>> = _sessionChatters
    internal val chatterRecency = ChatterRecency()

    /** Messages as they arrive in live chat, without recent chat history. */
    internal val liveIrcMessages: SharedFlow<ChatMessage> = client.messages
    val connectionState: StateFlow<ChatConnectionState> = client.state
    internal val roomModes: StateFlow<ChatRoomModes> = client.roomModes

    /** Shows the modes Twitch answered a moderator's change with, ahead of the ROOMSTATE that follows it. */
    internal val setRoomModes: (ChatRoomModes) -> Unit = client::setRoomModes

    private val badges = ChannelBadges(viewModelScope)
    val badgeUrls: StateFlow<Map<String, String>> = badges.urls

    private val emotes = ChannelEmotes(viewModelScope, onChannelResolved = ::listenEmoteEvents)
    val sevenTvEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.sevenTv
    val ffzEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.ffz
    val bttvEmotes: StateFlow<Map<String, SevenTvEmote>> = emotes.bttv

    private val cheermotes = ChannelCheermotes(viewModelScope)

    private val colors = ParticipantColors()
    val nickColors: StateFlow<Map<String, Color>> = colors.colors

    internal val pins = ChatPins(viewModelScope, { currentChannelId }, colors::remember)
    val pinnedChat: StateFlow<PinnedChat?> = pins.pinned
    /** Takes the phrases that hide messages in this channel now, whose rows the chat keeps beside the shown ones. */
    internal val trim = ChatTrim()
    internal val raid = OutgoingRaidTracker(viewModelScope, pubSub, { currentChannelId }, _messages, trim)
    internal val outgoingRaid: StateFlow<OutgoingRaid?> = raid.raid
    private val liveEvents = ChannelEventsController(viewModelScope)
    internal val channelEvents: StateFlow<ChannelEvents> = liveEvents.events

    private val recentChat = RecentChatLoad(
        viewModelScope,
        _messages,
        trim,
        ChatterTracking(_sessionChatters, chatterRecency, _chatterHistory),
        currentChannel = { currentChannel },
        mentionSelf = { mentionSelf },
    )
    internal val recentChatLoadState: StateFlow<RecentChatLoadState> = recentChat.state
    private val notices = ChatConnectionNotices(
        _messages,
        trim,
        onWelcomed = recentChat::start,
        onReconnected = recentChat::fillReconnectGap,
    )
    internal val reconnecting: StateFlow<Boolean> = notices.reconnecting
    private val deletions = ChatDeletions(_messages, _chatterHistory, trim)

    private var collectJob: Job? = null
    private val reconnect = ChatReconnect(
        viewModelScope,
        currentChannel = { currentChannel },
        connect = client::connect,
    )
    private var currentChannel: String? = null
    private var currentChannelId: String? = null
    private var raidToken: String? = null
    private var mentionSelf = ChatMentionSelf()
    private var inForeground = true
    private var pendingRewardEvents: List<ChatMessage> = emptyList()

    init {
        collectJob = viewModelScope.launch {
            merge(
                client.messages,
                pubSub.messages,
                sevenTvEvents.messages,
                bttvEvents.messages,
            ).collect { message ->
                when (message.eventKind) {
                    ChatEventKind.MessageDeleted -> deletions.markMessageDeleted(message)
                    ChatEventKind.UserMessagesDeleted -> deletions.markUserMessagesDeleted(message)
                    else -> {
                        val enriched = applyLiveEmoteChange(
                            CheermoteMatcher.apply(message.withKnownReward(currentChannel), cheermotes.tiers.value),
                        ) ?: return@collect
                        val seen = rememberSessionChatter(_sessionChatters.value, enriched)
                        if (seen.chatters !== _sessionChatters.value) {
                            _sessionChatters.value = seen.chatters
                        }
                        chatterRecency.record(enriched)
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
                                colors.paintReward(chatterMessage),
                                MAX_KEPT_CHAT_MESSAGES,
                                pendingRewardEvents,
                            )
                            pendingRewardEvents = result.pending
                            result.colorSource?.let { colors.remember(it) }
                            trim.keep(result.messages).also { updatedMessages = it }
                        }
                        _chatterHistory.update { history ->
                            rememberChatterHistory(history, previousMessages, updatedMessages, mentionSelf)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var previous: ChatConnectionState? = null
            client.state.collect { state ->
                // In the background only a finished connection changes the chat; the notices wait for the screen.
                if (inForeground || state == ChatConnectionState.Connected) notices.onState(previous, state)
                previous = state
                if (state == ChatConnectionState.Reconnecting && inForeground) {
                    reconnect.schedule()
                }
            }
        }
        viewModelScope.launch {
            pubSub.pins.collect { pins.onUpdate(it) }
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
    }

    internal fun setRaidToken(token: String?) {
        val normalized = token?.takeIf { it.isNotBlank() }
        if (raidToken == normalized) return
        raidToken = normalized
        currentChannelId?.let { pubSub.listen(it, normalized) }
    }

    /** Who the signed-in user is, for the mentions on their own chatter card. */
    internal fun setMentionSelf(self: ChatMentionSelf) {
        mentionSelf = self
    }

    internal fun hideChannelEvent(id: String) = liveEvents.hide(id)

    /** Rows another live source adds or changes, such as messages held for moderators. */
    internal fun editMessages(transform: (List<ChatMessage>) -> List<ChatMessage>) {
        _messages.update { current -> trim.keep(transform(current)) }
    }

    fun connect(channel: String, loadRecentChat: Boolean) {
        val normalized = channel.lowercase().removePrefix("#")
        if (normalized == currentChannel &&
            connectionState.value == ChatConnectionState.Connected
        ) {
            pins.revealHidden()
            return
        }
        val sameChannel = normalized == currentChannel
        if (!sameChannel) {
            _chatterHistory.value = ChatterHistory()
            sevenTvEvents.disconnect()
            bttvEvents.disconnect()
            pubSub.disconnect()
            currentChannelId = null
            raid.dismiss()
            liveEvents.clear()
        }
        pins.reopen(sameChannel)
        currentChannel = normalized
        reconnect.cancel()
        notices.reset()
        pendingRewardEvents = emptyList()
        _sessionChatters.value = emptyMap()
        chatterRecency.clear()
        _messages.value = listOf(chatNoticeMessage(ChatNotice.Connecting, CHAT_WELCOME_NOTICE_ID))
        recentChat.reset(loadOnConnect = loadRecentChat)
        colors.clear()
        val cachedId = ChannelAvatarRepository.cached(normalized)?.id
        currentChannelId = cachedId
        client.connect(normalized)
        cachedId?.let(::followChannel)
        viewModelScope.launch {
            val lookup = ChannelAvatarRepository.lookup(normalized)
            val channelId = (lookup as? ChannelLookup.Found)?.profile?.id
            if (channelId != null && currentChannel == normalized) {
                followChannel(channelId)
            } else if (currentChannel == normalized) {
                channelLookupNotice(lookup)?.let { notice ->
                    _messages.update { current -> trim.keep(withChannelLookupNotice(current, notice)) }
                }
            }
        }
        badges.open(normalized)
        cheermotes.open(normalized) { loaded ->
            _messages.update { messages ->
                messages.map { CheermoteMatcher.apply(it, loaded) }
            }
            _chatterHistory.update { history ->
                history.map { CheermoteMatcher.apply(it, loaded) }
            }
        }
        emotes.open(normalized)
    }

    fun refreshEmotes(): Job = emotes.refresh()

    fun setForeground(foreground: Boolean) {
        if (inForeground == foreground) return
        inForeground = foreground
        reconnect.cancel()
        if (!foreground) {
            client.pause()
            pubSub.disconnect()
            sevenTvEvents.disconnect()
            bttvEvents.disconnect()
            return
        }
        val channel = currentChannel ?: return
        client.connect(channel)
        ChannelAvatarRepository.cached(channel)?.id?.let(::followChannel)
        emotes.loadIfIncomplete(ChannelAvatarRepository.cached(channel)?.id)
    }

    /** Follows the channel's live sources: PubSub, the emote events, the pinned message, and the channel's events. */
    private fun followChannel(channelId: String) {
        currentChannelId = channelId
        pubSub.listen(channelId, raidToken)
        listenEmoteEvents(channelId)
        pins.load(channelId)
        liveEvents.load(channelId)
    }

    private fun listenEmoteEvents(channelId: String?) {
        if (channelId.isNullOrBlank() || !inForeground) return
        currentChannelId = channelId
        SevenTvRepository.channelEmoteSetId(channelId)?.let { sevenTvEvents.listen(it) }
        bttvEvents.listen(channelId)
    }

    private fun applyLiveEmoteChange(message: ChatMessage): ChatMessage? {
        val colored = colors.resolveEmoteActor(message)
        val change = colored.emoteChange
        val channelId = currentChannelId
        return when {
            change == null -> message
            channelId == null -> colored
            else -> emotes.applyLiveChange(channelId, colored, change)
        }
    }

    override fun onCleared() {
        reconnect.cancel()
        collectJob?.cancel()
        pins.close()
        sevenTvEvents.close()
        bttvEvents.close()
        pubSub.close()
        client.disconnect()
    }
}

/** A reward redemption with the title and cost the open [channel]'s known rewards give it. */
private fun ChatMessage.withKnownReward(channel: String?): ChatMessage {
    val rewardId = reward?.id ?: return this
    if (channel == null) return this
    return mergeKnownReward(this, ChannelCustomRewards.reward(channel, rewardId))
}

/** The chat with the notice why the channel could not be found, put before the welcome. */
private fun withChannelLookupNotice(messages: List<ChatMessage>, notice: ChatNotice): List<ChatMessage> =
    insertRecentChatBeforeWelcome(
        messages = messages,
        recent = listOf(chatNoticeMessage(notice, "channel-not-found-${System.nanoTime()}")),
        initialNoticeId = CHAT_WELCOME_NOTICE_ID,
        maxMessages = MAX_KEPT_CHAT_MESSAGES,
    )
