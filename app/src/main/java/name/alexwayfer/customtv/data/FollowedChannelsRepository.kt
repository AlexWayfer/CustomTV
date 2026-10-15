package name.alexwayfer.customtv.data

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
import kotlin.time.Duration.Companion.seconds

/** The log tag of the followed channels: their list, its saved copy, and the user's own follow. */
internal const val FOLLOWED_CHANNELS_TAG = "FollowedChannels"

internal class FollowedChannelsRepository(
    private val clientId: String,
    private val http: OkHttpClient = sharedHttpClient.newBuilder()
        .connectTimeout(10.seconds)
        .readTimeout(10.seconds)
        .build(),
) {
    suspend fun load(
        accessToken: String,
        userId: String,
        onList: (List<FollowedChannel>) -> Unit,
    ): FollowedChannelsResult {
        return try {
            val read = withContext(Dispatchers.IO) {
                readAllFollowed(accessToken, userId) { page ->
                    onList(orderedFollowedChannels(page))
                }
            }
            when (read) {
                is FollowedRead.Failed -> read.failure
                is FollowedRead.OkLive -> FollowedChannelsResult.Unavailable
                is FollowedRead.Ok -> if (read.channels.isEmpty()) {
                    FollowedChannelsResult.Ready(emptyList())
                } else {
                    loadDetails(accessToken, read.channels, onList)
                }
            }
        } catch (error: IOException) {
            AppLog.w(FOLLOWED_CHANNELS_TAG, "follows failed ${error.javaClass.simpleName}")
            FollowedChannelsResult.Unavailable
        }
    }

    /** Avatars, who is live, and the last broadcasts of [channels], each shown through [onList] once it is in. */
    private suspend fun loadDetails(
        accessToken: String,
        channels: List<FollowedChannel>,
        onList: (List<FollowedChannel>) -> Unit,
    ): FollowedChannelsResult {
        val ids = channels.map { it.id }.distinct()
        return coroutineScope {
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
            AppLog.w(FOLLOWED_CHANNELS_TAG, "follows failed ${error.javaClass.simpleName}")
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
                repeat(FOLLOWED_PAGE_LIMIT) { _ ->
                    val response = http.helixGet(liveFollowedStreamsUrl(userId, cursor), accessToken, clientId)
                    if (response.code == HTTP_TOO_MANY_REQUESTS) {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows rate limited")
                        return@withContext LiveFollowedRead.RateLimited(
                            helixRateLimitResetMillis(response.rateLimitReset),
                        )
                    }
                    if (response.code in HTTP_CLIENT_ERROR) {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows rejected HTTP ${response.code}")
                        return@withContext LiveFollowedRead.Rejected(response.code, twitchAuthMessage(response.body))
                    }
                    if (response.code !in HTTP_SUCCESS) {
                        AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows failed HTTP ${response.code}")
                        return@withContext LiveFollowedRead.Failed
                    }
                    val page = parseLiveFollowedStreamsPage(response.body)
                        ?: return@withContext LiveFollowedRead.Failed
                    channels += page.channels
                    cursor = page.cursor
                    if (cursor.isNullOrBlank()) return@withContext LiveFollowedRead.Ready(channels.distinctBy { it.id })
                }
                AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows truncated")
                LiveFollowedRead.Ready(channels.distinctBy { it.id })
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows failed ${error.javaClass.simpleName}")
                LiveFollowedRead.Failed
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
            AppLog.w(FOLLOWED_CHANNELS_TAG, "live follows failed ${error.javaClass.simpleName}")
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
        var pages = 0
        do {
            val response = http.helixGet(followedChannelsUrl(userId, cursor), accessToken, clientId)
            val failure = failureOf(response, "helix/channels/followed", checkScope = true)
            val page = if (failure == null) parseFollowedPage(response.body) else null
            if (page == null) return FollowedRead.Failed(failure ?: FollowedChannelsResult.Unavailable)
            channels += page.channels
            onPage(channels.distinctBy { it.id })
            cursor = page.cursor?.takeUnless { it.isBlank() }
            pages++
        } while (cursor != null && pages < FOLLOWED_PAGE_LIMIT)
        if (cursor != null) AppLog.w(FOLLOWED_CHANNELS_TAG, "followed list truncated")
        return FollowedRead.Ok(channels.distinctBy { it.id })
    }

    private fun readLiveResult(accessToken: String, userIds: List<String>): FollowedRead {
        val live = mutableMapOf<String, FollowedLive>()
        for (batch in userIds.chunked(FOLLOWED_PAGE_SIZE)) {
            val response = http.helixGet(followedStreamsUrl(batch), accessToken, clientId)
            val failure = failureOf(response, "helix/streams", checkScope = false)
            val parsed = if (failure == null) parseFollowedLive(response.body) else null
            if (parsed == null) return FollowedRead.Failed(failure ?: FollowedChannelsResult.Unavailable)
            live += parsed
        }
        // Helix names a category only in English; the rest of the app shows it in the app's language.
        val names = CategoryNames.localized(live.values.mapNotNull { it.categoryId })
        return FollowedRead.OkLive(localizedFollowedLive(live, names))
    }

    private fun followedUserDetails(logins: List<String>): FollowedUserDetails {
        val dates = mutableMapOf<String, Long>()
        val collaborations = mutableMapOf<String, FollowedCollaboration>()
        val fetched = mutableSetOf<String>()
        for (batch in logins.distinct().chunked(FOLLOWED_PAGE_SIZE)) {
            val body = lastBroadcastsBody(batch) ?: continue
            val parsedDates = parseLastBroadcasts(body)
            val parsedCollaborations = parseFollowedCollaborations(body)
            if (parsedDates != null && parsedCollaborations != null) {
                dates += parsedDates
                collaborations += parsedCollaborations
                fetched += batch.map { it.lowercase() }
            }
        }
        return FollowedUserDetails(dates, collaborations, fetched)
    }

    /** The answer about the last broadcasts and shared streams of [logins]; null when it failed. */
    private fun lastBroadcastsBody(logins: List<String>): String? {
        val request = twitchGqlRequest(lastBroadcastsQuery(logins)).build()
        return try {
            http.newCall(request).execute().use { response ->
                val text = response.body.string()
                Diagnostics.reportGql(FOLLOWED_CHANNELS_TAG, "last broadcasts", response.code, text)
                if (response.code !in HTTP_SUCCESS) {
                    AppLog.w(FOLLOWED_CHANNELS_TAG, "last broadcast failed HTTP ${response.code}")
                    null
                } else {
                    text
                }
            }
        } catch (error: IOException) {
            AppLog.w(FOLLOWED_CHANNELS_TAG, "last broadcast failed ${error.javaClass.simpleName}")
            null
        }
    }

    private fun readAvatars(accessToken: String, userIds: List<String>): Map<String, String> {
        val avatars = mutableMapOf<String, String>()
        for (batch in userIds.chunked(FOLLOWED_PAGE_SIZE)) {
            val response = http.helixGet(followedUsersUrl(batch), accessToken, clientId)
            val failed = failureOf(response, "helix/users", checkScope = false) != null
            val parsed = if (failed) null else parseFollowedAvatars(response.body)
            if (parsed == null) break
            avatars += parsed
        }
        return avatars
    }

    private fun failureOf(response: HelixBody, endpoint: String, checkScope: Boolean): FollowedChannelsResult? {
        val code = response.code
        if (code in HTTP_SUCCESS) return null
        if (checkScope && helixRejectsFollowScope(code, response.body)) {
            AppLog.w(FOLLOWED_CHANNELS_TAG, "follows rejected HTTP $code")
            return FollowedChannelsResult.NeedsPermission
        }
        if (code in HTTP_CLIENT_ERROR) {
            AppLog.w(FOLLOWED_CHANNELS_TAG, "follows rejected HTTP $code")
            Diagnostics.reportHttp(
                FOLLOWED_CHANNELS_TAG,
                endpoint,
                code,
                twitchAuthMessage(response.body),
                HELIX_EXPECTED_CODES,
            )
            return FollowedChannelsResult.Rejected
        }
        AppLog.w(FOLLOWED_CHANNELS_TAG, "follows failed HTTP $code")
        return FollowedChannelsResult.Unavailable
    }

    private sealed interface FollowedRead {
        data class Ok(val channels: List<FollowedChannel>) : FollowedRead
        data class OkLive(val live: Map<String, FollowedLive>) : FollowedRead
        data class Failed(val failure: FollowedChannelsResult) : FollowedRead
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
