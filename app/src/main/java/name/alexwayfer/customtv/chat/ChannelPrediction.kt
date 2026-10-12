package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.instantMillisOrNull
import name.alexwayfer.customtv.data.trimmedText
import org.json.JSONArray
import org.json.JSONObject

/**
 * A prediction shows while viewers can predict, and again with its result. Once predictions
 * close, while the streamer picks the outcome, and after a cancel, it is hidden.
 */
internal enum class ChannelPredictionStatus {
    Active,
    Resolved,
    Hidden,
}

internal data class ChannelPredictionOutcome(
    val id: String,
    val title: String,
    val points: Long,
    val users: Int,
)

internal data class ChannelPrediction(
    val id: String,
    val title: String,
    val status: ChannelPredictionStatus,
    val outcomes: List<ChannelPredictionOutcome>,
    val winningOutcomeId: String?,
    val windowMillis: Long,
    val endsAtMillis: Long,
)

internal object ChannelPredictionParser {
    /**
     * A `predictions-channel-v1.<channel id>` PubSub frame. The window end counts from the
     * frame's own server time, so a phone clock that is off does not move the countdown.
     */
    fun parseFrame(text: String, nowMillis: Long): ChannelPrediction? {
        val message = pubSubMessage(text) ?: return null
        val type = message.optString("type")
        if (type != "event-created" && type != "event-updated") return null
        val data = message.optJSONObject("data") ?: return null
        val event = data.optJSONObject("event") ?: return null
        val serverNow = instant(data.trimmedText("timestamp")) ?: nowMillis
        return parse(
            event = event,
            keys = PUB_SUB_KEYS,
            winningOutcomeId = event.trimmedText("winning_outcome_id"),
            clockOffsetMillis = nowMillis - serverNow,
        )
    }

    /** One of GQL `channel.activePredictionEvents`; the window end uses the phone clock. */
    fun parseGql(event: JSONObject?): ChannelPrediction? {
        if (event == null) return null
        return parse(
            event = event,
            keys = GQL_KEYS,
            winningOutcomeId = event.optJSONObject("winningOutcome")?.trimmedText("id"),
            clockOffsetMillis = 0L,
        )
    }

    /** The field names of one source of predictions. */
    private data class PredictionKeys(val created: String, val window: String, val points: String, val users: String)

    private val PUB_SUB_KEYS = PredictionKeys(
        created = "created_at",
        window = "prediction_window_seconds",
        points = "total_points",
        users = "total_users",
    )

    private val GQL_KEYS = PredictionKeys(
        created = "createdAt",
        window = "predictionWindowSeconds",
        points = "totalPoints",
        users = "totalUsers",
    )

    private fun parse(
        event: JSONObject,
        keys: PredictionKeys,
        winningOutcomeId: String?,
        clockOffsetMillis: Long,
    ): ChannelPrediction? {
        val id = event.trimmedText("id") ?: return null
        val windowMillis = event.optLong(keys.window).coerceAtLeast(0L) * 1_000L
        val createdAt = instant(event.trimmedText(keys.created))
        return ChannelPrediction(
            id = id,
            title = event.trimmedText("title").orEmpty(),
            status = status(event.optString("status")),
            outcomes = parseOutcomes(event.optJSONArray("outcomes"), keys),
            winningOutcomeId = winningOutcomeId,
            windowMillis = windowMillis,
            endsAtMillis = createdAt?.let { it + windowMillis + clockOffsetMillis } ?: 0L,
        )
    }

    private fun parseOutcomes(array: JSONArray?, keys: PredictionKeys): List<ChannelPredictionOutcome> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val outcome = array.optJSONObject(index) ?: return@mapNotNull null
            val id = outcome.trimmedText("id") ?: return@mapNotNull null
            ChannelPredictionOutcome(
                id = id,
                title = outcome.trimmedText("title").orEmpty(),
                points = outcome.optLong(keys.points).coerceAtLeast(0L),
                users = outcome.optInt(keys.users).coerceAtLeast(0),
            )
        }
    }

    private fun status(raw: String): ChannelPredictionStatus = when (raw.uppercase()) {
        "ACTIVE" -> ChannelPredictionStatus.Active
        "RESOLVED" -> ChannelPredictionStatus.Resolved
        else -> ChannelPredictionStatus.Hidden
    }

    private fun instant(raw: String?): Long? =
        raw?.let(::instantMillisOrNull)
}
