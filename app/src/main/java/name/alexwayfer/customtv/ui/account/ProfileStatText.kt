package name.alexwayfer.customtv.ui.account

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import name.alexwayfer.customtv.data.RecentLastLive
import name.alexwayfer.customtv.data.recentLastLive
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Where [value] sits in [text], a translated line built around it; null when the translation left it out. */
internal fun profileStatValueRange(text: String, value: String): IntRange? {
    if (value.isEmpty()) return null
    val start = text.indexOf(value)
    return if (start < 0) null else start until start + value.length
}

/** How the profile names when the channel was last live: by its distance while recent, by its date after a week. */
internal sealed interface LastLiveDay {
    data class Recent(val age: RecentLastLive) : LastLiveDay
    data class DaysAgo(val days: Int) : LastLiveDay
    data object OnDate : LastLiveDay
}

internal fun lastLiveDay(lastLiveMillis: Long, nowMillis: Long, zone: ZoneId): LastLiveDay {
    recentLastLive(lastLiveMillis, nowMillis, zone)?.let { return LastLiveDay.Recent(it) }
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(lastLiveMillis).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
    )
    return if (days <= LAST_LIVE_RELATIVE_DAYS) LastLiveDay.DaysAgo(days.toInt()) else LastLiveDay.OnDate
}

private const val LAST_LIVE_RELATIVE_DAYS = 7

/** [text] with [value] in bold, as the profile shows a follower count or a date. */
internal fun profileStatText(text: String, value: String): AnnotatedString = buildAnnotatedString {
    val range = profileStatValueRange(text, value)
    append(profileStatUnbrokenValue(text, range))
    range?.let { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1) }
}

/** [text] with the spaces in [valueRange] made non-breaking, so a date wraps whole, not between its month and year. */
internal fun profileStatUnbrokenValue(text: String, valueRange: IntRange?): String =
    if (valueRange == null) {
        text
    } else {
        text.replaceRange(valueRange, text.substring(valueRange).replace(' ', NO_BREAK_SPACE))
    }

private const val NO_BREAK_SPACE = '\u00A0'
