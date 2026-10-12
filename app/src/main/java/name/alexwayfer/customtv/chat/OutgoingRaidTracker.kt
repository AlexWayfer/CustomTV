package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.diagnostics.AppLog

/** The raid started from the open channel, followed through PubSub and checked again after a reconnect. */
internal class OutgoingRaidTracker(
    scope: CoroutineScope,
    pubSub: TwitchPubSubClient,
    private val currentChannelId: () -> String?,
    private val messages: MutableStateFlow<List<ChatMessage>>,
) {
    private val _raid = MutableStateFlow<OutgoingRaid?>(null)
    val raid: StateFlow<OutgoingRaid?> = _raid

    init {
        scope.launch {
            pubSub.raids.collect { event ->
                val current = _raid.value
                val createdId = outgoingRaidCreatedId(current, event)
                _raid.value = applyRaidEvent(current, event)
                if (createdId != null) appendRaidCreated(createdId)
            }
        }
        scope.launch {
            pubSub.opened.collect { channelId ->
                if (channelId != currentChannelId() || _raid.value == null) return@collect
                val lookup = OutgoingRaidLookup.load(channelId)
                if (channelId != currentChannelId()) return@collect
                _raid.value = recheckOutgoingRaid(_raid.value, lookup, System.currentTimeMillis())
                AppLog.i("Raid", "raid recheck after reconnect: ${lookup.javaClass.simpleName}")
            }
        }
    }

    fun dismiss() {
        _raid.value = null
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
        messages.update { current ->
            if (current.any { it.id == message.id }) current
            else (current + message).takeLast(MAX_CHAT_MESSAGES)
        }
    }
}
