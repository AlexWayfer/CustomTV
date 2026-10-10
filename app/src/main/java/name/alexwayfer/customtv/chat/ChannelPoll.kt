package name.alexwayfer.customtv.chat

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Active polls count down; ended ones show their results until Twitch archives them. */
internal enum class ChannelPollStatus {
    Active,
    Ended,
    Hidden,
}

internal data class ChannelPollChoice(val id: String, val title: String, val votes: Int)

internal data class ChannelPoll(
    val id: String,
    val title: String,
    val status: ChannelPollStatus,
    val choices: List<ChannelPollChoice>,
    val totalVotes: Int,
    val durationMillis: Long,
    val endsAtMillis: Long,
)

/** A choice's share of all votes, from 0 to 1. */
internal fun pollChoiceShare(votes: Int, totalVotes: Int): Float =
    if (totalVotes <= 0) 0f else (votes.toFloat() / totalVotes).coerceIn(0f, 1f)

internal fun pollChoicePercent(votes: Int, totalVotes: Int): Int = (pollChoiceShare(votes, totalVotes) * 100).roundToInt()

/** The choices with the most votes once the poll has votes; a tie has several leaders. */
internal fun pollLeaderIds(poll: ChannelPoll): Set<String> {
    val most = poll.choices.maxOfOrNull { it.votes } ?: return emptySet()
    if (most <= 0) return emptySet()
    return poll.choices.filter { it.votes == most }.mapTo(mutableSetOf()) { it.id }
}

internal object ChannelPollParser {
    /** A `polls.<channel id>` PubSub frame; the remaining time counts from [nowMillis]. */
    fun parseFrame(text: String, nowMillis: Long): ChannelPoll? {
        val message = pubSubMessage(text) ?: return null
        val type = message.optString("type")
        val poll = message.optJSONObject("data")?.optJSONObject("poll") ?: return null
        val parsed = parse(
            poll = poll,
            idKey = "poll_id",
            durationKey = "duration_seconds",
            remainingKey = "remaining_duration_milliseconds",
            choiceIdKey = "choice_id",
            nowMillis = nowMillis,
        ) ?: return null
        return if (type == "POLL_ARCHIVE") parsed.copy(status = ChannelPollStatus.Hidden) else parsed
    }

    /** GQL `user.viewablePoll`. */
    fun parseGql(poll: JSONObject?, nowMillis: Long): ChannelPoll? {
        if (poll == null) return null
        return parse(
            poll = poll,
            idKey = "id",
            durationKey = "durationSeconds",
            remainingKey = "remainingDurationMilliseconds",
            choiceIdKey = "id",
            nowMillis = nowMillis,
        )
    }

    private fun parse(
        poll: JSONObject,
        idKey: String,
        durationKey: String,
        remainingKey: String,
        choiceIdKey: String,
        nowMillis: Long,
    ): ChannelPoll? {
        val id = jsonText(poll, idKey) ?: return null
        val choices = parseChoices(poll.optJSONArray("choices"), choiceIdKey)
        val remaining = poll.optLong(remainingKey).coerceAtLeast(0L)
        return ChannelPoll(
            id = id,
            title = jsonText(poll, "title").orEmpty(),
            status = status(poll.optString("status")),
            choices = choices,
            totalVotes = votes(poll).takeIf { it > 0 } ?: choices.sumOf { it.votes },
            durationMillis = poll.optLong(durationKey).coerceAtLeast(0L) * 1_000L,
            endsAtMillis = nowMillis + remaining,
        )
    }

    private fun parseChoices(array: JSONArray?, idKey: String): List<ChannelPollChoice> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val choice = array.optJSONObject(index) ?: return@mapNotNull null
            val id = jsonText(choice, idKey) ?: return@mapNotNull null
            ChannelPollChoice(id = id, title = jsonText(choice, "title").orEmpty(), votes = votes(choice))
        }
    }

    private fun votes(json: JSONObject): Int = json.optJSONObject("votes")?.optInt("total")?.coerceAtLeast(0) ?: 0

    private fun status(raw: String): ChannelPollStatus = when (raw.uppercase()) {
        "ACTIVE" -> ChannelPollStatus.Active
        "COMPLETED", "TERMINATED" -> ChannelPollStatus.Ended
        else -> ChannelPollStatus.Hidden
    }
}

internal const val POLL_UPDATE = "POLL_UPDATE"

/** Updates of one poll go to the log at most this often: a busy poll sends one with every vote. */
internal const val POLL_UPDATE_LOG_INTERVAL_MILLIS = 10_000L

/**
 * A poll frame goes to the log unless it is an update of the poll whose update was logged last, less than
 * [POLL_UPDATE_LOG_INTERVAL_MILLIS] ago.
 */
internal fun pollFrameLogged(
    type: String,
    pollId: String?,
    nowMillis: Long,
    lastLoggedUpdatePollId: String?,
    lastLoggedUpdateAtMillis: Long,
): Boolean =
    type != POLL_UPDATE || pollId == null || pollId != lastLoggedUpdatePollId ||
        nowMillis - lastLoggedUpdateAtMillis >= POLL_UPDATE_LOG_INTERVAL_MILLIS

/** The inner message of a PubSub `MESSAGE` frame, which Twitch sends as a JSON string. */
internal fun pubSubMessage(text: String): JSONObject? {
    val frame = runCatching { JSONObject(text) }.getOrNull() ?: return null
    if (frame.optString("type") != "MESSAGE") return null
    val raw = frame.optJSONObject("data")?.optString("message").orEmpty()
    return runCatching { JSONObject(raw) }.getOrNull()
}

internal fun jsonText(json: JSONObject?, key: String): String? =
    json?.optString(key)?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

/** An open poll keeps the streamer's order; an ended one, like on Twitch, goes from most votes to least, ties as set. */
internal fun pollChoicesInOrder(poll: ChannelPoll): List<ChannelPollChoice> =
    if (poll.status == ChannelPollStatus.Active) poll.choices else poll.choices.sortedByDescending { it.votes }
