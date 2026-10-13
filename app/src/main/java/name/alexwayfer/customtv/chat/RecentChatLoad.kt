package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The chat history loaded once a channel's chat first connects, put before the welcome notice. */
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

    /** A channel opens; its history loads on the first connection when [loadOnConnect]. */
    fun reset(loadOnConnect: Boolean) {
        _state.value = RecentChatLoadState.Idle
        this.loadOnConnect = loadOnConnect
        started = false
    }

    fun start() {
        val channel = currentChannel() ?: return
        if (!loadOnConnect || started) return
        started = true
        _state.value = RecentChatLoadState.Loading
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
                if (currentChannel() == channel) _state.value = RecentChatLoadState.Idle
            }
        }
    }
}
