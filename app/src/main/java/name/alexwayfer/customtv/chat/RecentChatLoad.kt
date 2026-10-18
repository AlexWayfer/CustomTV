package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog

/**
 * The chat history loaded once a channel's chat first connects, put before the welcome notice, and again after a
 * reconnect, to put back what the lost connection missed.
 */
internal class RecentChatLoad(
    private val scope: CoroutineScope,
    private val messages: MutableStateFlow<List<ChatMessage>>,
    private val sessionChatters: MutableStateFlow<Map<String, String>>,
    private val chatterRecency: ChatterRecency,
    private val chatterHistory: MutableStateFlow<ChatterHistory>,
    private val currentChannel: () -> String?,
    private val mentionSelf: () -> ChatMentionSelf,
) {
    private val _state = MutableStateFlow(RecentChatLoadState.Idle)
    val state: StateFlow<RecentChatLoadState> = _state
    private var loadOnConnect = false
    private var started = false
    private var gapJob: Job? = null
    private var historyLoading = false
    private var gapLoading = false

    /** A channel opens; its history loads on the first connection when [loadOnConnect]. */
    fun reset(loadOnConnect: Boolean) {
        this.loadOnConnect = loadOnConnect
        started = false
        gapJob?.cancel()
        historyLoading = false
        gapLoading = false
        publishState()
    }

    private fun publishState() {
        _state.value = if (historyLoading || gapLoading) RecentChatLoadState.Loading else RecentChatLoadState.Idle
    }

    /**
     * The chat has just reconnected: loads the history again and puts back what came while the connection was down,
     * when history loads at all. A newer reconnect replaces the one under way.
     */
    fun fillReconnectGap() {
        val channel = currentChannel() ?: return
        if (!loadOnConnect) return
        val disconnectId = reconnectGapNoticeId(messages.value) ?: return
        gapJob?.cancel()
        gapLoading = true
        publishState()
        // Lazy, so the job is known before it can end.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                putBackGap(channel, disconnectId)
            } finally {
                // A replaced load ends after the next one has started; that one keeps the bar.
                if (gapJob === coroutineContext.job) {
                    gapLoading = false
                    publishState()
                }
            }
        }
        gapJob = job
        job.start()
    }

    private suspend fun putBackGap(channel: String, disconnectId: String) {
        val result = loadRecentChatMessages(channel, MAX_CHAT_MESSAGES)
        if (currentChannel() != channel) return
        var previous = emptyList<ChatMessage>()
        var merge: ReconnectGapMerge? = null
        messages.update { current ->
            previous = current
            merge = mergeReconnectGap(
                current,
                result.messages,
                disconnectId,
                sessionChatters.value,
                MAX_CHAT_MESSAGES,
            )
            merge?.messages ?: current
        }
        val applied = merge ?: return
        AppLog.i(RECENT_CHAT_TAG, "reconnect gap filled with ${applied.added.size} messages")
        sessionChatters.value = applied.chatters
        applied.added.forEach(chatterRecency::record)
        chatterHistory.update { history ->
            rememberChatterHistory(history, previous, applied.messages, mentionSelf())
        }
    }

    fun start() {
        val channel = currentChannel() ?: return
        if (!loadOnConnect || started) return
        started = true
        historyLoading = true
        publishState()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = loadRecentChatMessages(channel)
                if (currentChannel() == channel) {
                    val failure = recentChatNotice(result)?.let { notice ->
                        chatNoticeMessage(notice, "recent-chat-failed-${System.nanoTime()}")
                    }
                    var merge: RecentChatSessionMerge? = null
                    messages.update { current ->
                        val next = mergeRecentChatSession(result.messages, current, sessionChatters.value)
                        merge = next
                        insertRecentChatBeforeWelcome(
                            next.current,
                            next.recent + listOfNotNull(failure),
                            CHAT_WELCOME_NOTICE_ID,
                            MAX_CHAT_MESSAGES,
                        )
                    }
                    merge?.let { applied ->
                        sessionChatters.value = applied.chatters
                        applied.recent.forEach(chatterRecency::record)
                        chatterHistory.update { history ->
                            prependChatterHistory(history, applied.recent, applied.movedFirstLogins, mentionSelf())
                        }
                    }
                }
            } finally {
                if (currentChannel() == channel) {
                    historyLoading = false
                    publishState()
                }
            }
        }
    }
}
