package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.ChatterFollow
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

internal const val FOLLOWED_PAGE_SIZE = 100
internal const val FOLLOWED_PAGE_LIMIT = 20

internal data class FollowedChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isLive: Boolean = false,
    val categoryName: String? = null,
    val viewerCount: Int? = null,
    val sharedViewerCount: Int? = null,
    val collaborationCount: Int? = null,
    val collaboratorAvatarUrls: List<String> = emptyList(),
    val lastBroadcastAtMillis: Long? = null,
    val streamTitle: String? = null,
    /** When the shown stream preview was taken; a new value loads a new preview. Null while offline. */
    val previewAtMillis: Long? = null,
)

internal data class FollowedCollaboration(
    val sharedViewerCount: Int? = null,
    val collaboratorCount: Int? = null,
    val collaboratorAvatarUrls: List<String> = emptyList(),
)

internal data class FollowedUserDetails(
    val lastBroadcastAtMillis: Map<String, Long> = emptyMap(),
    val collaborationByLogin: Map<String, FollowedCollaboration> = emptyMap(),
    val collaborationFetchedLogins: Set<String> = emptySet(),
)

internal data class FollowedLive(
    val categoryName: String?,
    val viewerCount: Int,
    val title: String = "",
)

internal data class FollowedPage(
    val channels: List<FollowedChannel>,
    val cursor: String?,
)

internal fun userSubscriptionUrl(userId: String, broadcasterId: String): String {
    return helixUrl("subscriptions/user") {
        addQueryParameter("user_id", userId)
        addQueryParameter("broadcaster_id", broadcasterId)
    }
}

internal sealed class UserSubscriptionRead {
    data object Subscribed : UserSubscriptionRead()
    data object NotSubscribed : UserSubscriptionRead()
    data object Unavailable : UserSubscriptionRead()
}

internal fun userSubscriptionRead(httpCode: Int, body: String): UserSubscriptionRead {
    if (httpCode == 404) return UserSubscriptionRead.NotSubscribed
    if (httpCode in 200..299) {
        val listed = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("data")?.length() ?: 0
        return if (listed > 0) UserSubscriptionRead.Subscribed else UserSubscriptionRead.NotSubscribed
    }
    return UserSubscriptionRead.Unavailable
}

internal fun ownFollowedAtUrl(userId: String, broadcasterId: String): String {
    return helixUrl("channels/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("broadcaster_id", broadcasterId)
    }
}

internal sealed class OwnFollowRead {
    data class Following(val atMillis: Long) : OwnFollowRead()
    data object NotFollowing : OwnFollowRead()
    data object Unavailable : OwnFollowRead()
}

/** The card's follow line from an own follow read: an empty answer is Not following, a failed one is unknown. */
internal fun chatterFollowOf(read: OwnFollowRead): ChatterFollow? = when (read) {
    is OwnFollowRead.Following -> ChatterFollow.Following(read.atMillis)
    OwnFollowRead.NotFollowing -> ChatterFollow.NotFollowing
    OwnFollowRead.Unavailable -> null
}

internal fun parseOwnFollowedAt(body: String, broadcasterId: String): Long? {
    val data = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("data") ?: return null
    for (index in 0 until data.length()) {
        val item = data.optJSONObject(index) ?: continue
        if (item.optString("broadcaster_id") != broadcasterId) continue
        val raw = item.optString("followed_at")
        if (raw.isBlank() || raw == "null") return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }
    return null
}

internal fun followedChannelsUrl(userId: String, cursor: String?): String {
    return helixUrl("channels/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor)
    }
}

internal fun streamByLoginUrl(login: String): String {
    return helixUrl("streams") {
        addQueryParameter("user_login", login)
    }
}

internal fun followedStreamsUrl(userIds: List<String>): String {
    return helixUrl("streams") {
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        userIds.forEach { addQueryParameter("user_id", it) }
    }
}

/** Only the followed channels that are live, so one page covers a follow list of any size. */
internal fun liveFollowedStreamsUrl(userId: String, cursor: String?): String {
    return helixUrl("streams/followed") {
        addQueryParameter("user_id", userId)
        addQueryParameter("first", FOLLOWED_PAGE_SIZE.toString())
        if (!cursor.isNullOrBlank()) addQueryParameter("after", cursor)
    }
}

internal fun followedUsersUrl(userIds: List<String>): String {
    return helixUrl("users") {
        userIds.forEach { addQueryParameter("id", it) }
    }
}

internal fun helixRejectsFollowScope(httpCode: Int, body: String): Boolean {
    return httpCode == 401 && body.contains("scope", ignoreCase = true)
}

internal fun parseFollowedPage(body: String): FollowedPage? {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val channels = buildList {
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val id = item.optString("broadcaster_id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val login = item.optString("broadcaster_login").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val displayName = item.optString("broadcaster_name").takeIf { it.isNotBlank() && it != "null" } ?: login
            add(FollowedChannel(id = id, login = login, displayName = displayName))
        }
    }
    val cursor = root.optJSONObject("pagination")
        ?.optString("cursor")
        ?.takeIf { it.isNotBlank() && it != "null" }
    return FollowedPage(channels, cursor)
}

internal fun parseFollowedLive(body: String): Map<String, FollowedLive>? {
    val data = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("data") ?: return null
    return buildMap {
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            if (!item.optString("type").equals("live", ignoreCase = true)) continue
            val id = item.optString("user_id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val category = item.optString("game_name").takeIf { it.isNotBlank() && it != "null" }
            val title = item.optString("title").trim().let { if (it.isBlank() || it == "null") "" else it }
            put(id, FollowedLive(categoryName = category, viewerCount = item.optInt("viewer_count"), title = title))
        }
    }
}

internal fun parseLiveFollowedStreamsPage(body: String): FollowedPage? {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val data = root.optJSONArray("data") ?: return null
    val channels = buildList {
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            if (!item.optString("type").equals("live", ignoreCase = true)) continue
            val id = item.optString("user_id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val login = item.optString("user_login").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val displayName = item.optString("user_name").takeIf { it.isNotBlank() && it != "null" } ?: login
            val title = item.optString("title").trim().let { if (it.isBlank() || it == "null") "" else it }
            add(
                FollowedChannel(
                    id = id,
                    login = login,
                    displayName = displayName,
                    isLive = true,
                    categoryName = item.optString("game_name").takeIf { it.isNotBlank() && it != "null" },
                    viewerCount = item.optInt("viewer_count"),
                    streamTitle = title,
                ),
            )
        }
    }
    val cursor = root.optJSONObject("pagination")
        ?.optString("cursor")
        ?.takeIf { it.isNotBlank() && it != "null" }
    return FollowedPage(channels, cursor)
}

/** Helix sends the end of its rate-limit window as Unix seconds in `Ratelimit-Reset`. */
internal fun helixRateLimitResetMillis(header: String?): Long? {
    val seconds = header?.trim()?.toLongOrNull() ?: return null
    return seconds.takeIf { it > 0 }?.times(1_000)
}

internal fun parseFollowedAvatars(body: String): Map<String, String>? {
    val data = runCatching { JSONObject(body) }.getOrNull()?.optJSONArray("data") ?: return null
    return buildMap {
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val avatar = item.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" } ?: continue
            put(id, avatar)
        }
    }
}

internal fun mergeFollowedChannels(
    followed: List<FollowedChannel>,
    avatarsById: Map<String, String>,
    liveById: Map<String, FollowedLive>,
    lastBroadcastByLogin: Map<String, Long> = emptyMap(),
    collaborationByLogin: Map<String, FollowedCollaboration> = emptyMap(),
): List<FollowedChannel> {
    return orderedFollowedChannels(
        followed.map { channel ->
            val live = liveById[channel.id]
            val collaboration = collaborationByLogin[channel.login.lowercase()]
            channel.copy(
                avatarUrl = avatarsById[channel.id] ?: channel.avatarUrl,
                isLive = live != null,
                categoryName = live?.categoryName,
                streamTitle = live?.title,
                viewerCount = live?.viewerCount,
                sharedViewerCount = if (live != null) collaboration?.sharedViewerCount else null,
                collaborationCount = if (live != null) collaboration?.collaboratorCount else null,
                collaboratorAvatarUrls = if (live != null) collaboration?.collaboratorAvatarUrls.orEmpty() else emptyList(),
                lastBroadcastAtMillis = lastBroadcastByLogin[channel.login.lowercase()],
            )
        },
    )
}

internal fun refreshFollowedLive(
    channels: List<FollowedChannel>,
    liveById: Map<String, FollowedLive>,
    collaborationByLogin: Map<String, FollowedCollaboration>? = null,
    collaborationFetchedLogins: Set<String>? = null,
): List<FollowedChannel> {
    return orderedFollowedChannels(
        channels.map { channel ->
            val live = liveById[channel.id]
            val login = channel.login.lowercase()
            val fetched = collaborationFetchedLogins?.contains(login) ?: (collaborationByLogin != null)
            val collaboration = collaborationByLogin?.get(login)
            channel.copy(
                isLive = live != null,
                categoryName = live?.categoryName,
                streamTitle = live?.title,
                viewerCount = live?.viewerCount,
                sharedViewerCount = when {
                    live == null -> null
                    !fetched -> channel.sharedViewerCount
                    else -> collaboration?.sharedViewerCount
                },
                collaborationCount = when {
                    live == null -> null
                    !fetched -> channel.collaborationCount
                    else -> collaboration?.collaboratorCount
                },
                collaboratorAvatarUrls = when {
                    live == null -> emptyList()
                    !fetched -> channel.collaboratorAvatarUrls
                    else -> collaboration?.collaboratorAvatarUrls.orEmpty()
                },
            )
        },
    )
}

internal fun orderedFollowedChannels(channels: List<FollowedChannel>): List<FollowedChannel> {
    return channels.sortedWith(
        compareBy<FollowedChannel> { followedSortGroup(it) }
            .thenByDescending { channel ->
                when {
                    channel.isLive -> (channel.viewerCount ?: 0).toLong()
                    else -> channel.lastBroadcastAtMillis ?: 0L
                }
            }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            .thenBy { it.login },
    )
}

internal fun followedLastBroadcastMillis(channel: FollowedChannel): Long? {
    if (channel.isLive) return null
    return channel.lastBroadcastAtMillis
}

internal fun lastBroadcastsQuery(logins: List<String>): String {
    return JSONObject()
        .put(
            "query",
            $$"query($logins:[String!]!){users(logins:$logins){login lastBroadcast{startedAt} stream{viewersCount collaborationViewersCount} channel{collaboration{collaborators{status user{login profileImageURL(width:150)}}}}}}",
        )
        .put("variables", JSONObject().put("logins", JSONArray(logins)))
        .toString()
}

internal fun parseFollowedCollaborations(body: String): Map<String, FollowedCollaboration>? {
    val users = followedUsers(body) ?: return null
    return buildMap {
        for (index in 0 until users.length()) {
            val user = users.optJSONObject(index) ?: continue
            val login = user.optString("login").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val stream = user.optJSONObject("stream")
            val own = stream?.let { value ->
                if (!value.has("viewersCount") || value.isNull("viewersCount")) null else value.optInt("viewersCount")
            }
            val shared = stream?.let { value ->
                if (!value.has("collaborationViewersCount") || value.isNull("collaborationViewersCount")) {
                    null
                } else {
                    value.optInt("collaborationViewersCount")
                }
            }
            val collaborators = user.optJSONObject("channel")
                ?.optJSONObject("collaboration")
                ?.optJSONArray("collaborators")
            val collaboration = FollowedCollaboration(
                sharedViewerCount = sharedStreamViewerCount(own, shared),
                collaboratorCount = collaborators?.let {
                    collaborationOthersCount(activeCollaboratorLogins(it), login)
                },
                collaboratorAvatarUrls = collaborators?.let { activeCollaboratorAvatarUrls(it, login) }.orEmpty(),
            )
            if (collaboration.sharedViewerCount == null && collaboration.collaboratorCount == null) continue
            put(login.lowercase(), collaboration)
        }
    }
}

private fun followedUsers(body: String): JSONArray? {
    return runCatching { JSONObject(body) }.getOrNull()
        ?.optJSONObject("data")
        ?.optJSONArray("users")
}

internal fun parseLastBroadcasts(body: String): Map<String, Long>? {
    val users = followedUsers(body) ?: return null
    return buildMap {
        for (index in 0 until users.length()) {
            val user = users.optJSONObject(index) ?: continue
            val login = user.optString("login").takeIf { it.isNotBlank() && it != "null" } ?: continue
            val startedAt = user.optJSONObject("lastBroadcast")?.optString("startedAt") ?: continue
            val millis = parseLastBroadcastMillis(startedAt) ?: continue
            put(login.lowercase(), millis)
        }
    }
}

internal fun parseLastBroadcastMillis(startedAt: String): Long? {
    return try {
        Instant.parse(startedAt).toEpochMilli()
    } catch (_: DateTimeException) {
        null
    }
}

internal sealed interface FollowedLastBroadcastAge {
    data object Today : FollowedLastBroadcastAge
    data object Yesterday : FollowedLastBroadcastAge
    data class DaysAgo(val days: Int) : FollowedLastBroadcastAge
    data class MonthsAgo(val months: Int) : FollowedLastBroadcastAge
    data class YearsAgo(val years: Int) : FollowedLastBroadcastAge
}

internal fun followedLastBroadcastAge(
    lastBroadcastAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): FollowedLastBroadcastAge {
    val broadcastDay = Instant.ofEpochMilli(lastBroadcastAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(broadcastDay, today).toInt()
    if (days <= 1) return if (days == 1) FollowedLastBroadcastAge.Yesterday else FollowedLastBroadcastAge.Today
    val months = ChronoUnit.MONTHS.between(broadcastDay, today).toInt()
    if (months < 1) return FollowedLastBroadcastAge.DaysAgo(days)
    val years = ChronoUnit.YEARS.between(broadcastDay, today).toInt()
    if (years < 1) return FollowedLastBroadcastAge.MonthsAgo(months)
    return FollowedLastBroadcastAge.YearsAgo(years)
}

private fun followedSortGroup(channel: FollowedChannel): Int {
    return when {
        channel.isLive -> 0
        channel.lastBroadcastAtMillis != null -> 1
        else -> 2
    }
}

private fun helixUrl(path: String, fill: okhttp3.HttpUrl.Builder.() -> Unit): String {
    return "https://api.twitch.tv/helix/$path".toHttpUrl().newBuilder().apply(fill).build().toString()
}
