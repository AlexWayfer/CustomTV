package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.data.ChannelProfile

fun recentProfilesWithCache(
    current: Map<String, ChannelProfile>,
    history: List<String>,
    cached: (String) -> ChannelProfile?,
): Map<String, ChannelProfile> {
    val updates = history.mapNotNull { login ->
        cached(login)?.let { login.lowercase() to it }
    }.toMap()
    return if (updates.isEmpty()) current else current + updates
}
