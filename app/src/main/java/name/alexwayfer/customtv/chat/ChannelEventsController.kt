package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.data.ChannelEventsRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * How long an ended prediction or hype train stays: the minute Twitch keeps an ended poll before
 * archiving it. Twitch sends no event that hides either of them.
 */
private val ENDED_EVENT_SHOWN = 1.minutes

/**
 * When a shown hype train hides by itself: an ended one after [ENDED_EVENT_SHOWN], an approach
 * once its time to start runs out. A running train waits for Twitch's end event.
 */
internal fun hypeTrainHidesAfter(train: ChannelHypeTrain, nowMillis: Long): Duration? = when (train.phase) {
    HypeTrainPhase.Ended -> ENDED_EVENT_SHOWN
    HypeTrainPhase.Approaching -> (train.endsAtMillis - nowMillis).coerceAtLeast(0L).milliseconds
    HypeTrainPhase.Active -> null
}

/**
 * The channel's poll, prediction, and hype train over chat. A load brings the ones already
 * running; PubSub brings every change after that. A load that finishes after a PubSub change of
 * the same kind is older than it, so it does not replace it.
 */
internal class ChannelEventsController(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> ChannelEvents? = ChannelEventsRepository::fetch,
    private val wait: suspend (Duration) -> Unit = { delay(it) },
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _events = MutableStateFlow(ChannelEvents())
    val events: StateFlow<ChannelEvents> = _events

    private var channelId: String? = null
    private var poll: ChannelPoll? = null
    private var prediction: ChannelPrediction? = null
    private var hypeTrain: ChannelHypeTrain? = null
    private val hiddenIds = mutableSetOf<String>()
    private val liveIds = mutableSetOf<String>()
    private var pollGeneration = 0
    private var predictionGeneration = 0
    private var hypeTrainGeneration = 0
    private var loadJob: Job? = null
    private val predictionHide = HideTimer()
    private val hypeTrainHide = HideTimer()

    fun load(channelId: String) {
        if (this.channelId != channelId) clear()
        this.channelId = channelId
        val pollStarted = pollGeneration
        val predictionStarted = predictionGeneration
        val hypeTrainStarted = hypeTrainGeneration
        loadJob?.cancel()
        loadJob = scope.launch {
            val loaded = fetch(channelId) ?: return@launch
            if (this@ChannelEventsController.channelId != channelId) return@launch
            if (pollGeneration == pollStarted) poll = loaded.poll
            if (predictionGeneration == predictionStarted) prediction = loaded.prediction
            if (hypeTrainGeneration == hypeTrainStarted) hypeTrain = loaded.hypeTrain
            publish()
        }
    }

    fun clear() {
        loadJob?.cancel()
        predictionHide.cancel()
        hypeTrainHide.cancel()
        channelId = null
        poll = null
        prediction = null
        hypeTrain = null
        hiddenIds.clear()
        liveIds.clear()
        pollGeneration++
        predictionGeneration++
        hypeTrainGeneration++
        publish()
    }

    fun onPoll(update: ChannelPoll) {
        pollGeneration++
        liveIds += update.id
        poll = applyPollUpdate(poll, update)
        publish()
    }

    fun onPrediction(update: ChannelPrediction) {
        predictionGeneration++
        liveIds += update.id
        prediction = applyPredictionUpdate(prediction, update)
        publish()
        val current = prediction
        val after = if (current?.status == ChannelPredictionStatus.Resolved) ENDED_EVENT_SHOWN else null
        predictionHide.schedule(current?.id.takeIf { after != null }, after) {
            if (prediction?.id == current?.id) {
                prediction = null
                publish()
            }
        }
    }

    fun onHypeTrain(event: HypeTrainEvent) {
        hypeTrainGeneration++
        hypeTrain = applyHypeTrainEvent(hypeTrain, event)
        hypeTrain?.let { liveIds += it.id }
        publish()
        val current = hypeTrain
        val after = current?.let { hypeTrainHidesAfter(it, now()) }
        // Keyed by phase too: the end of a train replaces the timer of its approach.
        hypeTrainHide.schedule(current?.let { "${it.id}:${it.phase}" }.takeIf { after != null }, after) {
            if (hypeTrain?.id == current?.id && hypeTrain?.phase == current?.phase) {
                hypeTrain = null
                publish()
            }
        }
    }

    /** Hides this poll, prediction, or hype train for this viewer; a new one shows again. */
    fun hide(id: String) {
        hiddenIds += id
        publish()
    }

    private fun publish() {
        _events.value = visibleChannelEvents(poll, prediction, hypeTrain, hiddenIds, liveIds.toSet())
    }

    /** One pending hide. A repeat of the same key keeps its first timer; another key replaces it. */
    private inner class HideTimer {
        private var job: Job? = null
        private var key: String? = null

        fun schedule(newKey: String?, after: Duration?, hide: () -> Unit) {
            if (newKey == null || after == null) {
                cancel()
                return
            }
            if (newKey == key) return
            cancel()
            key = newKey
            job = scope.launch {
                wait(after)
                hide()
            }
        }

        fun cancel() {
            job?.cancel()
            job = null
            key = null
        }
    }
}
