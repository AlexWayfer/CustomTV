package name.alexwayfer.customtv.chat

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** A hype train is about to start, runs level by level, or has ended and shows its last level. */
internal enum class HypeTrainPhase {
    Approaching,
    Active,
    Ended,
}

/** The viewer who gave the most in one currency during this train. */
internal data class HypeTrainConductor(val source: String, val displayName: String)

internal data class HypeTrainEmote(val id: String, val token: String)

internal data class ChannelHypeTrain(
    val id: String,
    val phase: HypeTrainPhase,
    val level: Int,
    val progress: Int,
    val goal: Int,
    val endsAtMillis: Long,
    val colorHex: String?,
    val conductors: List<HypeTrainConductor>,
    val rewards: List<HypeTrainEmote>,
    val eventsToStart: Int = 0,
)

/** The share of the current level already filled, from 0 to 100. */
internal fun hypeTrainPercent(train: ChannelHypeTrain): Int =
    if (train.goal <= 0) 0 else (train.progress * 100f / train.goal).roundToInt().coerceIn(0, 100)

internal sealed interface HypeTrainEvent {
    /**
     * Viewers are close to starting a train: [eventsRemaining] more before [endsAtMillis]. [colorHex]
     * is the streamer's creator color, which Twitch shows as a stripe on the approach.
     */
    data class Approaching(
        val eventsRemaining: Int,
        val endsAtMillis: Long,
        val rewards: List<HypeTrainEmote>,
        val colorHex: String? = null,
    ) : HypeTrainEvent

    /** The whole train, as a start, a level-up, or a load from GQL sends it. */
    data class Execution(val train: ChannelHypeTrain) : HypeTrainEvent

    /** A contribution moved the train along; it carries no color or conductors. */
    data class Progress(
        val id: String,
        val level: Int,
        val progress: Int,
        val goal: Int,
        val endsAtMillis: Long,
        val rewards: List<HypeTrainEmote>,
    ) : HypeTrainEvent

    data class Conductor(val trainId: String, val conductor: HypeTrainConductor) : HypeTrainEvent

    data class End(val id: String) : HypeTrainEvent
}

/**
 * How one event changes the train shown. An approach does not replace a running train. Progress
 * and conductors of a train that is not shown start it without the color, which the next level-up
 * brings. The end of another train leaves the current one.
 */
internal fun applyHypeTrainEvent(current: ChannelHypeTrain?, event: HypeTrainEvent): ChannelHypeTrain? = when (event) {
    is HypeTrainEvent.Approaching -> if (current?.phase == HypeTrainPhase.Active) {
        current
    } else {
        ChannelHypeTrain(
            id = APPROACHING_ID,
            phase = HypeTrainPhase.Approaching,
            level = 0,
            progress = 0,
            goal = 0,
            endsAtMillis = event.endsAtMillis,
            colorHex = event.colorHex,
            conductors = emptyList(),
            rewards = event.rewards,
            eventsToStart = event.eventsRemaining,
        )
    }
    is HypeTrainEvent.Execution -> event.train
    is HypeTrainEvent.Progress -> {
        val base = current?.takeIf { it.id == event.id }
        (base ?: ChannelHypeTrain(event.id, HypeTrainPhase.Active, 0, 0, 0, 0L, null, emptyList(), emptyList())).copy(
            phase = HypeTrainPhase.Active,
            level = event.level,
            progress = event.progress,
            goal = event.goal,
            endsAtMillis = event.endsAtMillis,
            rewards = event.rewards.ifEmpty { base?.rewards.orEmpty() },
        )
    }
    is HypeTrainEvent.Conductor -> if (current?.id == event.trainId) {
        current.copy(conductors = current.conductors.filter { it.source != event.conductor.source } + event.conductor)
    } else {
        current
    }
    is HypeTrainEvent.End -> if (current?.id == event.id) current.copy(phase = HypeTrainPhase.Ended) else current
}

internal object HypeTrainParser {
    /** A `hype-train-events-v2.<channel id>` PubSub frame; times count from [nowMillis]. */
    fun parseFrame(text: String, nowMillis: Long): HypeTrainEvent? {
        val message = pubSubMessage(text) ?: return null
        val data = message.optJSONObject("data") ?: return null
        return when (message.optString("type")) {
            "hype-train-approaching" -> approaching(data, nowMillis)
            "hype-train-start" -> execution(data, nowMillis)?.let(HypeTrainEvent::Execution)
            "hype-train-level-up" -> execution(data.optJSONObject("hype_train"), nowMillis)
                ?.let(HypeTrainEvent::Execution)
            "hype-train-progression" -> progress(data, nowMillis)
            "hype-train-conductor-update" -> conductor(data)
            "hype-train-end" -> jsonText(data, "id")?.let(HypeTrainEvent::End)
            else -> null
        }
    }

    /**
     * A running train in the shape both GQL `channel.hypeTrain.execution` and PubSub starts and
     * level-ups use. The end counts from the train's own update time, so a phone clock that is
     * off does not move the timer.
     */
    fun execution(json: JSONObject?, nowMillis: Long): ChannelHypeTrain? {
        if (json == null) return null
        val id = jsonText(json, "id") ?: return null
        val progress = json.optJSONObject("progress") ?: return null
        val level = progress.optJSONObject("level")
        val remainingMillis = if (progress.has("remainingSeconds")) {
            progress.optLong("remainingSeconds").coerceAtLeast(0L) * 1_000L
        } else {
            val expires = instant(jsonText(json, "expiresAt"))
            val updated = instant(jsonText(json, "updatedAt"))
            if (expires != null && updated != null) (expires - updated).coerceAtLeast(0L) else 0L
        }
        return ChannelHypeTrain(
            id = id,
            phase = if (jsonText(json, "endedAt") != null) HypeTrainPhase.Ended else HypeTrainPhase.Active,
            level = level?.optInt("value") ?: 0,
            progress = progress.optInt("progression").coerceAtLeast(0),
            goal = progress.optInt("goal").coerceAtLeast(0),
            endsAtMillis = nowMillis + remainingMillis,
            colorHex = jsonText(json.optJSONObject("config"), "primaryHexColor"),
            conductors = conductors(json.optJSONArray("conductors")),
            rewards = rewards(level?.optJSONArray("rewards")),
        )
    }

    /**
     * GQL `channel.hypeTrain.approaching`, so a train that is about to start shows when chat opens
     * too. Its end uses the phone clock. `eventsRemaining` reads as one object or a list.
     */
    fun gqlApproaching(json: JSONObject?, nowMillis: Long): ChannelHypeTrain? {
        if (json == null) return null
        val endsAt = instant(jsonText(json, "expiresAt"))?.takeIf { it > nowMillis } ?: return null
        val remaining = json.optJSONArray("eventsRemaining")
            ?.let { list -> (0 until list.length()).mapNotNull { list.optJSONObject(it)?.optInt("events") }.minOrNull() }
            ?: json.optJSONObject("eventsRemaining")?.optInt("events")
            ?: json.optInt("goal")
        val event = HypeTrainEvent.Approaching(
            eventsRemaining = remaining,
            endsAtMillis = endsAt,
            rewards = rewards(json.optJSONArray("levelOneRewards")),
            colorHex = jsonText(json, "creatorColor"),
        )
        return applyHypeTrainEvent(null, event)
    }

    private fun approaching(data: JSONObject, nowMillis: Long): HypeTrainEvent.Approaching? {
        val remaining = data.optJSONObject("events_remaining_durations") ?: return null
        val events = remaining.keys().asSequence().mapNotNull { it.toIntOrNull() }.minOrNull() ?: return null
        val seconds = remaining.optLong(events.toString()).coerceAtLeast(0L)
        return HypeTrainEvent.Approaching(
            eventsRemaining = events,
            endsAtMillis = nowMillis + seconds * 1_000L,
            rewards = rewards(data.optJSONArray("level_one_rewards")),
            colorHex = jsonText(data, "creator_color"),
        )
    }

    private fun progress(data: JSONObject, nowMillis: Long): HypeTrainEvent.Progress? {
        val id = jsonText(data, "id") ?: return null
        val progress = data.optJSONObject("progress") ?: return null
        val level = progress.optJSONObject("level")
        return HypeTrainEvent.Progress(
            id = id,
            level = level?.optInt("value") ?: 0,
            progress = progress.optInt("value").coerceAtLeast(0),
            goal = progress.optInt("goal").coerceAtLeast(0),
            endsAtMillis = nowMillis + progress.optLong("remaining_seconds").coerceAtLeast(0L) * 1_000L,
            rewards = rewards(level?.optJSONArray("rewards")),
        )
    }

    private fun conductor(data: JSONObject): HypeTrainEvent.Conductor? {
        val source = jsonText(data, "source") ?: return null
        val id = jsonText(data, "id")?.removeSuffix("-$source") ?: return null
        val user = data.optJSONObject("user") ?: return null
        val name = jsonText(user, "display_name") ?: jsonText(user, "login") ?: return null
        return HypeTrainEvent.Conductor(id, HypeTrainConductor(source, name))
    }

    private fun conductors(array: JSONArray?): List<HypeTrainConductor> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val conductor = array.optJSONObject(index) ?: return@mapNotNull null
            val source = jsonText(conductor, "source") ?: return@mapNotNull null
            val user = conductor.optJSONObject("user")
            val name = jsonText(user, "displayName") ?: jsonText(user, "login") ?: return@mapNotNull null
            HypeTrainConductor(source, name)
        }
    }

    /** Emote rewards, in either PubSub's flat shape or GQL's nested `emote`; other rewards are skipped. */
    private fun rewards(array: JSONArray?): List<HypeTrainEmote> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val reward = array.optJSONObject(index) ?: return@mapNotNull null
            val emote = reward.optJSONObject("emote") ?: reward.takeIf { it.optString("type") == "EMOTE" }
            val id = jsonText(emote, "id") ?: return@mapNotNull null
            HypeTrainEmote(id, jsonText(emote, "token").orEmpty())
        }
    }

    private fun instant(raw: String?): Long? =
        raw?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
}

private const val APPROACHING_ID = "approaching"
