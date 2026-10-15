package name.alexwayfer.customtv.data

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    /** The live category's ID, which stays the same in any language its name is shown in. */
    val categoryId: String? = null,
)

/** A live stream as Helix lists it; [categoryId] names its category in the app's language through [CategoryNames]. */
internal data class FollowedLive(
    val categoryName: String?,
    val viewerCount: Int,
    val title: String = "",
    val categoryId: String? = null,
)

/** [live] with each category named by [names], its ID's name in the app's language; Helix's English one otherwise. */
internal fun localizedFollowedLive(
    live: Map<String, FollowedLive>,
    names: Map<String, String>,
): Map<String, FollowedLive> =
    live.mapValues { (_, stream) ->
        stream.categoryId?.let(names::get)?.let { stream.copy(categoryName = it) } ?: stream
    }

internal data class FollowedPage(
    val channels: List<FollowedChannel>,
    val cursor: String?,
)

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
                categoryId = live?.categoryId,
                streamTitle = live?.title,
                viewerCount = live?.viewerCount,
                sharedViewerCount = if (live != null) collaboration?.sharedViewerCount else null,
                collaborationCount = if (live != null) collaboration?.collaboratorCount else null,
                collaboratorAvatarUrls =
                    if (live != null) collaboration?.collaboratorAvatarUrls.orEmpty() else emptyList(),
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
                categoryId = live?.categoryId,
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
