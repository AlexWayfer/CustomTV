package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.auth.twitchAuthMessage
import name.alexwayfer.customtv.diagnostics.AppLog
import name.alexwayfer.customtv.diagnostics.Diagnostics
import name.alexwayfer.customtv.diagnostics.HELIX_EXPECTED_CODES
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class FollowedChannelsRepository(
    private val clientId: String,
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun load(
        accessToken: String,
        userId: String,
        onList: (List<FollowedChannel>) -> Unit,
    ): FollowedChannelsResult {
        return try {
            val channels = when (
                val read = withContext(Dispatchers.IO) {
                    readAllFollowed(accessToken, userId) { page ->
                        onList(orderedFollowedChannels(page))
                    }
                }
            ) {
                is FollowedRead.Failed -> return read.failure
                is FollowedRead.OkLive -> return FollowedChannelsResult.Unavailable
                is FollowedRead.Ok -> read.channels
            }
            if (channels.isEmpty()) return FollowedChannelsResult.Ready(emptyList())
            val ids = channels.map { it.id }.distinct()
            coroutineScope {
                val avatarsJob = async(Dispatchers.IO) { readAvatars(accessToken, ids) }
                val liveJob = async(Dispatchers.IO) { readLiveResult(accessToken, ids) }
                val datesJob = async(Dispatchers.IO) { followedUserDetails(channels.map { it.login }) }
                val avatars = avatarsJob.await()
                onList(mergeFollowedChannels(channels, avatars, emptyMap()))
                val liveRead = liveJob.await()
                val live = if (liveRead is FollowedRead.OkLive) liveRead.live else emptyMap()
                onList(mergeFollowedChannels(channels, avatars, live))
                val details = datesJob.await()
                val ready = mergeFollowedChannels(
                    channels,
                    avatars,
                    live,
                    details.lastBroadcastAtMillis,
                    details.collaborationByLogin,
                )
                onList(ready)
                FollowedChannelsResult.Ready(ready, liveKnown = liveRead is FollowedRead.OkLive)
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "follows failed ${error.javaClass.simpleName}")
            FollowedChannelsResult.Unavailable
        }
    }

    /** Every followed channel, live or not, by ID, login, and name; null when the list could not be read. */
    suspend fun followedForAlerts(accessToken: String, userId: String): List<FollowedChannel>? {
        if (userId.isBlank()) return null
        return try {
            when (val read = withContext(Dispatchers.IO) { readAllFollowed(accessToken, userId) {} }) {
                is FollowedRead.Ok -> read.channels
                else -> null
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "follows failed ${error.javaClass.simpleName}")
            null
        }
    }

    /** Reads only the live follows, so one request covers up to 100 live channels at any follow count. */
    suspend fun liveFollowedForAlerts(accessToken: String, userId: String): LiveFollowedRead {
        if (userId.isBlank()) return LiveFollowedRead.Failed
        return withContext(Dispatchers.IO) {
            try {
                val channels = mutableListOf<FollowedChannel>()
                var cursor: String? = null
                repeat(FOLLOWED_PAGE_LIMIT) {
                    val response = get(accessToken, liveFollowedStreamsUrl(userId, cursor))
                    if (response.code == 429) {
                        AppLog.w(TAG, "live follows rate limited")
                        return@withContext LiveFollowedRead.RateLimited(
                            helixRateLimitResetMillis(response.rateLimitReset),
                        )
                    }
                    if (response.code in 400..499) {
                        AppLog.w(TAG, "live follows rejected HTTP ${response.code}")
                        return@withContext LiveFollowedRead.Rejected(response.code, twitchAuthMessage(response.body))
                    }
                    if (response.code !in 200..299) {
                        AppLog.w(TAG, "live follows failed HTTP ${response.code}")
                        return@withContext LiveFollowedRead.Failed
                    }
                    val page = parseLiveFollowedStreamsPage(response.body)
                        ?: return@withContext LiveFollowedRead.Failed
                    channels += page.channels
                    cursor = page.cursor
                    if (cursor.isNullOrBlank()) return@withContext LiveFollowedRead.Ready(channels.distinctBy { it.id })
                }
                AppLog.w(TAG, "live follows truncated")
                LiveFollowedRead.Ready(channels.distinctBy { it.id })
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "live follows failed ${error.javaClass.simpleName}")
                LiveFollowedRead.Failed
            }
        }
    }

    suspend fun ownChatterFollow(accessToken: String, userId: String, broadcasterId: String): ChatterFollow? =
        chatterFollowOf(readOwnFollow(accessToken, userId, broadcasterId))

    suspend fun readOwnSubscription(
        accessToken: String,
        userId: String,
        broadcasterId: String,
    ): UserSubscriptionRead {
        if (userId.isBlank() || broadcasterId.isBlank()) return UserSubscriptionRead.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val response = get(accessToken, userSubscriptionUrl(userId, broadcasterId))
                val read = userSubscriptionRead(response.code, response.body)
                if (read == UserSubscriptionRead.Unavailable) {
                    if (response.code in 400..499) {
                        AppLog.w(TAG, "subscription rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/subscriptions/user",
                            response.code,
                            twitchAuthMessage(response.body),
                            HELIX_EXPECTED_CODES,
                        )
                    } else {
                        AppLog.w(TAG, "subscription failed HTTP ${response.code}")
                    }
                }
                read
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "subscription failed ${error.javaClass.simpleName}")
                UserSubscriptionRead.Unavailable
            }
        }
    }

    suspend fun readOwnFollow(accessToken: String, userId: String, broadcasterId: String): OwnFollowRead {
        if (userId.isBlank() || broadcasterId.isBlank()) return OwnFollowRead.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val response = get(accessToken, ownFollowedAtUrl(userId, broadcasterId))
                when (response.code) {
                    in 200..299 -> {
                        val atMillis = parseOwnFollowedAt(response.body, broadcasterId)
                        if (atMillis == null) OwnFollowRead.NotFollowing else OwnFollowRead.Following(atMillis)
                    }
                    in 400..499 -> {
                        AppLog.w(TAG, "own follow rejected HTTP ${response.code}")
                        Diagnostics.reportHttp(
                            TAG,
                            "helix/channels/followed",
                            response.code,
                            twitchAuthMessage(response.body),
                            HELIX_EXPECTED_CODES,
                        )
                        OwnFollowRead.Unavailable
                    }
                    else -> {
                        AppLog.w(TAG, "own follow failed HTTP ${response.code}")
                        OwnFollowRead.Unavailable
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(TAG, "own follow failed ${error.javaClass.simpleName}")
                OwnFollowRead.Unavailable
            }
        }
    }

    fun refreshLive(accessToken: String, channels: List<FollowedChannel>): FollowedChannelsResult {
        if (channels.isEmpty()) return FollowedChannelsResult.Ready(channels)
        return try {
            val ids = channels.map { it.id }.distinct()
            when (val live = readLiveResult(accessToken, ids)) {
                is FollowedRead.Failed -> live.failure
                is FollowedRead.OkLive -> {
                    val liveLogins = channels.mapNotNull { channel ->
                        if (live.live.containsKey(channel.id)) channel.login else null
                    }
                    val details = followedUserDetails(liveLogins)
                    FollowedChannelsResult.Ready(
                        refreshFollowedLive(
                            channels,
                            live.live,
                            details.collaborationByLogin,
                            details.collaborationFetchedLogins,
                        ),
                    )
                }
                is FollowedRead.Ok -> FollowedChannelsResult.Unavailable
            }
        } catch (error: IOException) {
            AppLog.w(TAG, "live follows failed ${error.javaClass.simpleName}")
            FollowedChannelsResult.Unavailable
        }
    }

    private fun readAllFollowed(
        accessToken: String,
        userId: String,
        onPage: (List<FollowedChannel>) -> Unit,
    ): FollowedRead {
        val channels = mutableListOf<FollowedChannel>()
        var cursor: String? = null
        repeat(FOLLOWED_PAGE_LIMIT) {
            val response = get(accessToken, followedChannelsUrl(userId, cursor))
            failureOf(response, "helix/channels/followed", checkScope = true)?.let { return FollowedRead.Failed(it) }
            val page = parseFollowedPage(response.body) ?: return FollowedRead.Failed(FollowedChannelsResult.Unavailable)
            channels += page.channels
            val snapshot = channels.distinctBy { it.id }
            onPage(snapshot)
            val next = page.cursor
            if (next.isNullOrBlank()) return FollowedRead.Ok(snapshot)
            cursor = next
        }
        AppLog.w(TAG, "followed list truncated")
        return FollowedRead.Ok(channels.distinctBy { it.id })
    }

    private fun readLiveResult(accessToken: String, userIds: List<String>): FollowedRead {
        val live = mutableMapOf<String, FollowedLive>()
        for (batch in userIds.chunked(FOLLOWED_PAGE_SIZE)) {
            val response = get(accessToken, followedStreamsUrl(batch))
            failureOf(response, "helix/streams", checkScope = false)?.let { return FollowedRead.Failed(it) }
            val parsed = parseFollowedLive(response.body) ?: return FollowedRead.Failed(FollowedChannelsResult.Unavailable)
            live += parsed
        }
        return FollowedRead.OkLive(live)
    }

    private fun followedUserDetails(logins: List<String>): FollowedUserDetails {
        val dates = mutableMapOf<String, Long>()
        val collaborations = mutableMapOf<String, FollowedCollaboration>()
        val fetched = mutableSetOf<String>()
        for (batch in logins.distinct().chunked(FOLLOWED_PAGE_SIZE)) {
            val request = twitchGqlRequest(lastBroadcastsQuery(batch)).build()
            val body = try {
                http.newCall(request).execute().use { response ->
                    val text = response.body.string()
                    Diagnostics.reportGql(TAG, "last broadcasts", response.code, text)
                    if (response.code !in 200..299) {
                        AppLog.w(TAG, "last broadcast failed HTTP ${response.code}")
                        null
                    } else {
                        text
                    }
                }
            } catch (error: IOException) {
                AppLog.w(TAG, "last broadcast failed ${error.javaClass.simpleName}")
                null
            } ?: continue
            val parsedDates = parseLastBroadcasts(body) ?: continue
            val parsedCollaborations = parseFollowedCollaborations(body) ?: continue
            dates += parsedDates
            collaborations += parsedCollaborations
            fetched += batch.map { it.lowercase() }
        }
        return FollowedUserDetails(dates, collaborations, fetched)
    }

    private fun readAvatars(accessToken: String, userIds: List<String>): Map<String, String> {
        val avatars = mutableMapOf<String, String>()
        for (batch in userIds.chunked(FOLLOWED_PAGE_SIZE)) {
            val response = get(accessToken, followedUsersUrl(batch))
            if (failureOf(response, "helix/users", checkScope = false) != null) return avatars
            val parsed = parseFollowedAvatars(response.body) ?: return avatars
            avatars += parsed
        }
        return avatars
    }

    private fun failureOf(response: HelixBody, endpoint: String, checkScope: Boolean): FollowedChannelsResult? {
        val code = response.code
        if (code in 200..299) return null
        if (checkScope && helixRejectsFollowScope(code, response.body)) {
            AppLog.w(TAG, "follows rejected HTTP $code")
            return FollowedChannelsResult.NeedsPermission
        }
        if (code in 400..499) {
            AppLog.w(TAG, "follows rejected HTTP $code")
            Diagnostics.reportHttp(TAG, endpoint, code, twitchAuthMessage(response.body), HELIX_EXPECTED_CODES)
            return FollowedChannelsResult.Rejected
        }
        AppLog.w(TAG, "follows failed HTTP $code")
        return FollowedChannelsResult.Unavailable
    }

    private fun get(accessToken: String, url: String): HelixBody {
        val request = helixRequest(url, accessToken, clientId)
            .build()
        http.newCall(request).execute().use { response ->
            return HelixBody(response.code, response.body.string(), response.header("Ratelimit-Reset"))
        }
    }

    private data class HelixBody(val code: Int, val body: String, val rateLimitReset: String? = null)

    private sealed interface FollowedRead {
        data class Ok(val channels: List<FollowedChannel>) : FollowedRead
        data class OkLive(val live: Map<String, FollowedLive>) : FollowedRead
        data class Failed(val failure: FollowedChannelsResult) : FollowedRead
    }

    private companion object {
        const val TAG = "FollowedChannels"
    }
}

internal sealed interface LiveFollowedRead {
    data class Ready(val channels: List<FollowedChannel>) : LiveFollowedRead
    /** Helix refused the request for its rate limit; [retryAtMillis] is when the window resets. */
    data class RateLimited(val retryAtMillis: Long?) : LiveFollowedRead
    /** Helix refused this request; sending it again unchanged gets the same answer. */
    data class Rejected(val httpCode: Int, val message: String) : LiveFollowedRead
    /** The server or the network failed; the next check may succeed. */
    data object Failed : LiveFollowedRead
}

internal sealed interface FollowedChannelsResult {
    data class Ready(
        val channels: List<FollowedChannel>,
        val liveKnown: Boolean = true,
    ) : FollowedChannelsResult
    data object NeedsPermission : FollowedChannelsResult
    data object Rejected : FollowedChannelsResult
    data object Unavailable : FollowedChannelsResult
}
