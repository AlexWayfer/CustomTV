package name.alexwayfer.customtv.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.chat.ChannelEvents
import name.alexwayfer.customtv.chat.parseChannelEventsGql
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import org.json.JSONObject

/** Loads a channel's running poll, prediction, and hype train in one public GQL request, without a login. */
internal object ChannelEventsRepository {
    private val http = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Null when Twitch did not answer or refused the request. */
    suspend fun fetch(channelId: String): ChannelEvents? {
        if (channelId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("query", EVENTS_QUERY)
                    .put("variables", JSONObject().put("id", channelId))
                val request = twitchGqlRequest(payload.toString()).build()
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    Diagnostics.reportGql(TAG, "events", response.code, body)
                    when (response.code) {
                        in 200..299 -> parseChannelEventsGql(body, System.currentTimeMillis()).also {
                            if (it == null) AppLog.w(TAG, "events unreadable: ${gqlFailureCause(body)}")
                        }
                        in 400..499 -> {
                            AppLog.w(TAG, "events rejected HTTP ${response.code}")
                            null
                        }
                        else -> {
                            AppLog.w(TAG, "events failed HTTP ${response.code}")
                            null
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "events failed ${error.javaClass.simpleName}")
                null
            }
        }
    }

    private const val TAG = "ChannelEvents"
    private const val EVENTS_QUERY =
        $$"query($id:ID!){user(id:$id){" +
            "viewablePoll{id title status durationSeconds remainingDurationMilliseconds " +
            "choices{id title votes{total}} votes{total}} " +
            "channel{activePredictionEvents{id title status createdAt predictionWindowSeconds winningOutcome{id} " +
            "outcomes{id title totalPoints totalUsers}} " +
            "hypeTrain{execution{id updatedAt expiresAt endedAt config{primaryHexColor} " +
            "progress{goal progression total remainingSeconds " +
            "level{value goal rewards{... on HypeTrainEmoteReward{emote{id token}}}}} " +
            "conductors{source user{login displayName}}} " +
            "approaching{goal expiresAt creatorColor eventsRemaining{events} " +
            "levelOneRewards{... on HypeTrainEmoteReward{emote{id token}}}}}}}}"
}
