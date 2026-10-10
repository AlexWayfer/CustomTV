package name.alexwayfer.customtv.chat

import java.time.Instant
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
        val serverNow = instant(jsonText(data, "timestamp")) ?: nowMillis
        return parse(
            event = event,
            createdKey = "created_at",
            windowKey = "prediction_window_seconds",
            pointsKey = "total_points",
            usersKey = "total_users",
            winningOutcomeId = jsonText(event, "winning_outcome_id"),
            clockOffsetMillis = nowMillis - serverNow,
        )
    }

    /** One of GQL `channel.activePredictionEvents`; the window end uses the phone clock. */
    fun parseGql(event: JSONObject?): ChannelPrediction? {
        if (event == null) return null
        return parse(
            event = event,
            createdKey = "createdAt",
            windowKey = "predictionWindowSeconds",
            pointsKey = "totalPoints",
            usersKey = "totalUsers",
            winningOutcomeId = jsonText(event.optJSONObject("winningOutcome"), "id"),
            clockOffsetMillis = 0L,
        )
    }

    private fun parse(
        event: JSONObject,
        createdKey: String,
        windowKey: String,
        pointsKey: String,
        usersKey: String,
        winningOutcomeId: String?,
        clockOffsetMillis: Long,
    ): ChannelPrediction? {
        val id = jsonText(event, "id") ?: return null
        val windowMillis = event.optLong(windowKey).coerceAtLeast(0L) * 1_000L
        val createdAt = instant(jsonText(event, createdKey))
        return ChannelPrediction(
            id = id,
            title = jsonText(event, "title").orEmpty(),
            status = status(event.optString("status")),
            outcomes = parseOutcomes(event.optJSONArray("outcomes"), pointsKey, usersKey),
            winningOutcomeId = winningOutcomeId,
            windowMillis = windowMillis,
            endsAtMillis = createdAt?.let { it + windowMillis + clockOffsetMillis } ?: 0L,
        )
    }

    private fun parseOutcomes(array: JSONArray?, pointsKey: String, usersKey: String): List<ChannelPredictionOutcome> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val outcome = array.optJSONObject(index) ?: return@mapNotNull null
            val id = jsonText(outcome, "id") ?: return@mapNotNull null
            ChannelPredictionOutcome(
                id = id,
                title = jsonText(outcome, "title").orEmpty(),
                points = outcome.optLong(pointsKey).coerceAtLeast(0L),
                users = outcome.optInt(usersKey).coerceAtLeast(0),
            )
        }
    }

    private fun status(raw: String): ChannelPredictionStatus = when (raw.uppercase()) {
        "ACTIVE" -> ChannelPredictionStatus.Active
        "RESOLVED" -> ChannelPredictionStatus.Resolved
        else -> ChannelPredictionStatus.Hidden
    }

    private fun instant(raw: String?): Long? =
        raw?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
}
