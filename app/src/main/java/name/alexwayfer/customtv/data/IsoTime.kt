package name.alexwayfer.customtv.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** The epoch milliseconds of a time in UTC such as `2026-10-11T20:15:00Z`, or null when [text] is not one. */
internal fun instantMillisOrNull(text: String): Long? = try {
    Instant.parse(text).toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

/** [instantMillisOrNull] that also reads a time with an offset, such as `2026-10-11T23:15:00+03:00`. */
internal fun offsetTimeMillisOrNull(text: String): Long? = instantMillisOrNull(text) ?: try {
    OffsetDateTime.parse(text).toInstant().toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}
