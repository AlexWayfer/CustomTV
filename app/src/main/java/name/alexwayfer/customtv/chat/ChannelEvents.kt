package name.alexwayfer.customtv.chat

import org.json.JSONObject

/** The channel's poll, prediction, and hype train shown over chat. */
internal data class ChannelEvents(
    val poll: ChannelPoll? = null,
    val prediction: ChannelPrediction? = null,
    val hypeTrain: ChannelHypeTrain? = null,
    /** The ids that PubSub brought while chat was open, rather than the load when it opened. */
    val liveIds: Set<String> = emptySet(),
)

/**
 * A shown poll replaces the current one. A hidden one, such as Twitch's archive of an ended poll,
 * removes only the same poll; the end of another poll leaves the current one.
 */
internal fun applyPollUpdate(current: ChannelPoll?, update: ChannelPoll): ChannelPoll? = when {
    update.status != ChannelPollStatus.Hidden -> update
    current?.id == update.id -> null
    else -> current
}

/** The same rule for predictions: closed, pending, and canceled ones remove only themselves. */
internal fun applyPredictionUpdate(current: ChannelPrediction?, update: ChannelPrediction): ChannelPrediction? = when {
    update.status != ChannelPredictionStatus.Hidden -> update
    current?.id == update.id -> null
    else -> current
}

internal fun visibleChannelEvents(
    poll: ChannelPoll?,
    prediction: ChannelPrediction?,
    hypeTrain: ChannelHypeTrain?,
    hiddenIds: Set<String>,
    liveIds: Set<String>,
): ChannelEvents = ChannelEvents(
    poll = poll?.takeUnless { it.id in hiddenIds },
    prediction = prediction?.takeUnless { it.id in hiddenIds },
    hypeTrain = hypeTrain?.takeUnless { it.id in hiddenIds },
    liveIds = liveIds,
)

/**
 * The poll, prediction, and hype train already running when chat opens, from one public GQL
 * answer. Null when the answer could not be read; a hidden poll or prediction counts as none.
 */
internal fun parseChannelEventsGql(body: String, nowMillis: Long): ChannelEvents? {
    val user = runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONObject("user")
        ?: return null
    val poll = ChannelPollParser.parseGql(user.optJSONObject("viewablePoll"), nowMillis)
        ?.takeIf { it.status != ChannelPollStatus.Hidden }
    val channel = user.optJSONObject("channel")
    val events = channel?.optJSONArray("activePredictionEvents")
    val prediction = (0 until (events?.length() ?: 0)).firstNotNullOfOrNull { index ->
        ChannelPredictionParser.parseGql(events?.optJSONObject(index))
            ?.takeIf { it.status != ChannelPredictionStatus.Hidden }
    }
    val trains = channel?.optJSONObject("hypeTrain")
    val hypeTrain = HypeTrainParser.execution(trains?.optJSONObject("execution"), nowMillis)
        ?.takeIf { it.phase == HypeTrainPhase.Active }
        ?: HypeTrainParser.gqlApproaching(trains?.optJSONObject("approaching"), nowMillis)
    return ChannelEvents(poll, prediction, hypeTrain)
}
