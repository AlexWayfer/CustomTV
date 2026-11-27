package name.alexwayfer.customtv.data

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private const val DEFAULT_RETENTION_DAYS = 3
private const val DAYS_PER_WEEK = 7
private const val IMGDB_DAYS_PER_MONTH = 30

/** How long the image host keeps a newly uploaded chat image or album. */
enum class ChatImageRetention(val seconds: Long) {
    OneHour(1.hours.inWholeSeconds),
    OneDay(1.days.inWholeSeconds),
    ThreeDays(DEFAULT_RETENTION_DAYS.days.inWholeSeconds),
    OneWeek(DAYS_PER_WEEK.days.inWholeSeconds),
    OneMonth(IMGDB_DAYS_PER_MONTH.days.inWholeSeconds),
    NoExpiry(0),
}

internal fun storedChatImageRetention(value: String?): ChatImageRetention =
    ChatImageRetention.entries.firstOrNull { it.name == value } ?: ChatImageRetention.ThreeDays
