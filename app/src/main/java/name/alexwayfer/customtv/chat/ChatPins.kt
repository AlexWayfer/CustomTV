package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.PinnedChatRepository
import kotlin.time.Duration.Companion.milliseconds

/**
 * The message pinned in the open channel's chat as the viewer sees it: loaded, changed through PubSub, hidden for
 * the viewer alone, and gone once its time ends.
 */
internal class ChatPins(
    private val scope: CoroutineScope,
    private val currentChannelId: () -> String?,
    private val onPinMessage: (ChatMessage) -> Unit,
) {
    private val _pinned = MutableStateFlow<PinnedChat?>(null)
    val pinned: StateFlow<PinnedChat?> = _pinned
    private var expireJob: Job? = null
    private var currentPin: PinnedChat? = null
    private var hiddenPinId: String? = null
    private var generation = 0

    fun hideForSelf() {
        val pin = currentPin ?: return
        hiddenPinId = pin.pinId
        _pinned.value = null
    }

    fun revealHidden() {
        if (hiddenPinId == null) return
        hiddenPinId = null
        _pinned.value = currentPin
    }

    /** Shows the pin again when the same channel opens, and drops the last channel's pin when another one does. */
    fun reopen(sameChannel: Boolean) {
        hiddenPinId = null
        if (sameChannel) {
            publish()
        } else {
            generation++
            show(null)
        }
    }

    fun onUpdate(update: PinnedChatUpdate) {
        when (update) {
            is PinnedChatUpdate.Set -> {
                generation++
                show(update.pin.copy(live = true))
            }
            is PinnedChatUpdate.Clear -> {
                if (shouldClearPinnedChat(currentPin, update.pinId)) {
                    generation++
                    show(null)
                }
            }
            is PinnedChatUpdate.Duration -> onDuration(update)
            PinnedChatUpdate.Refresh -> {
                generation++
                currentChannelId()?.let(::load)
            }
        }
    }

    /** A new end time for the pin on screen; without one, the pin is loaded again. */
    private fun onDuration(update: PinnedChatUpdate.Duration) {
        val current = currentPin
        if (current == null) {
            generation++
            currentChannelId()?.let(::load)
            return
        }
        val updated = updatePinnedChatDuration(current, update.pinId, update.endsAtMillis)
        if (updated === current) return
        generation++
        show(updated)
    }

    fun load(channelId: String) {
        val loadGeneration = generation
        scope.launch {
            val pin = loadResult { PinnedChatRepository.fetch(channelId) }
                .getOrElse { return@launch }
            if (loadGeneration != generation || currentChannelId() != channelId) return@launch
            show(pin)
        }
    }

    private fun show(pin: PinnedChat?) {
        expireJob?.cancel()
        currentPin = pin
        if (pin != null && pin.pinId != hiddenPinId) {
            hiddenPinId = null
        }
        publish()
        pin?.message?.let(onPinMessage)
        val endsAt = pin?.endsAtMillis ?: return
        val wait = endsAt - System.currentTimeMillis()
        if (wait <= 0L) {
            currentPin = null
            publish()
            return
        }
        val pinId = pin.pinId
        expireJob = scope.launch {
            delay(wait.milliseconds)
            if (currentPin?.pinId == pinId) {
                currentPin = null
                publish()
            }
        }
    }

    private fun publish() {
        _pinned.value = visiblePinnedChat(currentPin, hiddenPinId)
    }

    fun close() {
        expireJob?.cancel()
    }
}
