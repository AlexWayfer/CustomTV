package name.alexwayfer.customtv.data

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** How a channel's last broadcast is named while it was today or yesterday; older ones count days or show dates. */
internal sealed interface RecentLastLive {
    data object JustNow : RecentLastLive
    data class MinutesAgo(val minutes: Int) : RecentLastLive
    data class HoursAgo(val hours: Int) : RecentLastLive
    data object Today : RecentLastLive
    data object Yesterday : RecentLastLive
}

/** A broadcast counts hours while fewer than these have passed, then names its day. */
private const val RECENT_HOURS_SHOWN = 12

/**
 * How long ago a broadcast that ended at [lastLiveMillis] was, while that was today or yesterday; null when it was
 * earlier. Twitch's `lastBroadcast.startedAt`, despite its name, is when that broadcast ended: it matches the end of
 * the broadcast's recording.
 */
internal fun recentLastLive(lastLiveMillis: Long, nowMillis: Long, zone: ZoneId): RecentLastLive? {
    val elapsed = (nowMillis - lastLiveMillis).coerceAtLeast(0L).milliseconds
    if (elapsed < 1.minutes) return RecentLastLive.JustNow
    if (elapsed < 1.hours) return RecentLastLive.MinutesAgo(elapsed.inWholeMinutes.toInt())
    if (elapsed < RECENT_HOURS_SHOWN.hours) return RecentLastLive.HoursAgo(elapsed.inWholeHours.toInt())
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(lastLiveMillis).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
    )
    return when (days) {
        0L -> RecentLastLive.Today
        1L -> RecentLastLive.Yesterday
        else -> null
    }
}
