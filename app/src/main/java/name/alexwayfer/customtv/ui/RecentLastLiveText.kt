package name.alexwayfer.customtv.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.data.RecentLastLive

/**
 * [age] of a broadcast that ended at [lastLiveMillis] in words; each screen gives its own words for today and
 * yesterday, which take that time, as one starts a line and one ends it.
 */
@Composable
internal fun recentLastLiveText(
    age: RecentLastLive,
    lastLiveMillis: Long,
    @StringRes justNow: Int,
    @StringRes todayAt: Int,
    @StringRes yesterdayAt: Int,
): String = when (age) {
    RecentLastLive.JustNow -> stringResource(justNow)
    is RecentLastLive.MinutesAgo -> pluralStringResource(R.plurals.last_live_minutes_ago, age.minutes, age.minutes)
    is RecentLastLive.HoursAgo -> pluralStringResource(R.plurals.last_live_hours_ago, age.hours, age.hours)
    RecentLastLive.Today -> stringResource(todayAt, formatChatTimestamp(LocalContext.current, lastLiveMillis))
    RecentLastLive.Yesterday -> stringResource(yesterdayAt, formatChatTimestamp(LocalContext.current, lastLiveMillis))
}
