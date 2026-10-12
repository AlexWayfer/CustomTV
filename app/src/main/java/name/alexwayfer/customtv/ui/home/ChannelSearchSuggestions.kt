package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.channelSearchExtras

// The empty field is a hint of whom to watch, not a second list of the follows.
/** How many followed channels an empty search field suggests, the live ones first; all live ones show even past it. */
internal const val EMPTY_SEARCH_FOLLOWED = 10

/** What the search suggests, in its order: the recents, then the follows, then the channels Twitch found. */
internal data class ChannelSearchMatches(
    val history: List<String>,
    val followed: List<FollowedChannel>,
    val found: List<ChannelSearchHit>,
)

/** The suggestions for the [query]: each list leaves out the channels an earlier one already shows. */
internal fun channelSearchMatches(
    history: List<String>,
    profiles: Map<String, ChannelProfile>,
    followed: List<FollowedChannel>,
    searchHits: List<ChannelSearchHit>,
    query: String,
): ChannelSearchMatches {
    val historyMatches = channelSearchSuggestions(history, profiles, query)
    val followedMatches = followedSearchSuggestions(followed, historyMatches, query)
    val found = channelSearchExtras(historyMatches + followedMatches.map { it.login }, searchHits)
    return ChannelSearchMatches(historyMatches, followedMatches, found)
}

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
