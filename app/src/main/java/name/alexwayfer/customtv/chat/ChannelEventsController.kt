package name.alexwayfer.customtv.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private val fetch: suspend (String) -> ChannelEvents?,
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
    private var themeJob: Job? = null
    private var lastThemeAttempt: Pair<String, Int>? = null
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
            val loaded = fetch(channelId)
            if (this@ChannelEventsController.channelId != channelId) return@launch
            if (loaded != null) {
                if (pollGeneration == pollStarted) poll = loaded.poll
                if (predictionGeneration == predictionStarted) prediction = loaded.prediction
                if (hypeTrainGeneration == hypeTrainStarted) updateHypeTrain(loaded.hypeTrain)
                else acceptTheme(loaded.hypeTrain)
            }
            if (hypeTrainGeneration != hypeTrainStarted && hypeTrain?.id != loaded?.hypeTrain?.id) loadMissingTheme()
            publish()
        }
    }

    fun clear() {
        loadJob?.cancel()
        themeJob?.cancel()
        lastThemeAttempt = null
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
        val updated = applyHypeTrainEvent(hypeTrain, event)
        updated?.let { liveIds += it.id }
        updateHypeTrain(updated)
        if (loadJob?.isActive != true) loadMissingTheme()
        publish()
    }

    /** PubSub sends the train without the stream categories. One lookup covers all its contributions. */
    private fun loadMissingTheme() {
        val channel = channelId ?: return
        val train = hypeTrain ?: return
        if (train.theme != null || train.phase == HypeTrainPhase.Ended) return
        val key = train.id to train.level
        if (lastThemeAttempt == key || lastThemeAttempt?.first == train.id && themeJob?.isActive == true) return
        themeJob?.cancel()
        lastThemeAttempt = key
        themeJob = scope.launch {
            val snapshot = fetch(channel)?.hypeTrain
            if (channelId == channel && snapshot?.id == train.id) {
                acceptTheme(snapshot)
                publish()
            }
        }
    }

    private fun acceptTheme(snapshot: ChannelHypeTrain?) {
        val current = hypeTrain ?: return
        if (snapshot?.id == current.id && snapshot.theme != null && current.theme == null) {
            hypeTrain = current.copy(theme = snapshot.theme)
        }
    }

    private fun updateHypeTrain(train: ChannelHypeTrain?) {
        val previous = hypeTrain
        if (previous?.id != train?.id) {
            themeJob?.cancel()
            lastThemeAttempt = null
        }
        hypeTrain = if (train != null && train.theme == null && previous?.id == train.id) {
            train.copy(theme = previous.theme)
        } else {
            train
        }
        val after = train?.let { hypeTrainHidesAfter(it, now()) }
        val key = when (train?.phase) {
            HypeTrainPhase.Approaching -> "approaching:${train.id}:${train.endsAtMillis}"
            HypeTrainPhase.Ended -> "ended:${train.id}"
            HypeTrainPhase.Active, null -> null
        }
        hypeTrainHide.schedule(key, after) {
            if (hypeTrain?.id == train?.id && hypeTrain?.phase == train?.phase) {
                updateHypeTrain(null)
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
