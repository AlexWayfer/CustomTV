package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.FollowedChannel

// The empty field is a hint of whom to watch, not a second list of the follows.
/** How many followed channels an empty search field suggests, the live ones first; all live ones show even past it. */
internal const val EMPTY_SEARCH_FOLLOWED = 10

/** Whether a channel the app knows matches the typed text: the start of its login or of its display name. */
internal fun channelMatchesSearch(login: String, displayName: String?, normalizedQuery: String): Boolean =
    login.lowercase().startsWith(normalizedQuery) ||
        displayName?.lowercase()?.startsWith(normalizedQuery) == true

/** The recent channels to suggest, in their order: all of them in an empty field, otherwise the matching ones. */
internal fun channelSearchSuggestions(
    history: List<String>,
    profiles: Map<String, ChannelProfile>,
    query: String,
): List<String> {
    val normalized = normalizeChannelInput(query)
    if (normalized.isEmpty()) return history
    return history.filter { channelMatchesSearch(it, profiles[it]?.displayName, normalized) }
}

/**
 * The followed channels to suggest below the recents, without those already [shown]: the matching ones while
 * typing, all of them in an empty field. The live ones come first by name, then the offline ones that streamed
 * last. An empty field stops at [EMPTY_SEARCH_FOLLOWED], but keeps every live one.
 */
internal fun followedSearchSuggestions(
    followed: List<FollowedChannel>,
    shown: Collection<String>,
    query: String,
): List<FollowedChannel> {
    val shownLogins = shown.mapTo(HashSet()) { it.lowercase() }
    val normalized = normalizeChannelInput(query)
    val byName = compareBy<FollowedChannel> { it.displayName.lowercase() }
    val (live, offline) = followed
        .filter { it.login.lowercase() !in shownLogins }
        .filter { normalized.isEmpty() || channelMatchesSearch(it.login, it.displayName, normalized) }
        .partition { it.isLive }
    val lastStreamed = offline.sortedWith(
        compareByDescending<FollowedChannel> { it.lastBroadcastAtMillis ?: Long.MIN_VALUE }.then(byName),
    )
    val offlineShown = if (normalized.isEmpty()) {
        lastStreamed.take((EMPTY_SEARCH_FOLLOWED - live.size).coerceAtLeast(0))
    } else {
        lastStreamed
    }
    return live.sortedWith(byName) + offlineShown
}
