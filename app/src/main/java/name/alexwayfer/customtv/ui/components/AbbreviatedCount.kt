package name.alexwayfer.customtv.ui.components

/**
 * A count shortened the way Twitch shows it, in English for every locale: `999`, `1.2K`, `12.1K`, `123K`, `4.5M`.
 * One decimal below 100 of a unit, none from 100; the decimal is cut, not rounded, so `11,999` stays `11.9K`
 * and never grows into the next unit.
 */
internal fun abbreviatedCount(value: Long): String {
    val (divisor, suffix) = when {
        value >= 1_000_000_000L -> 1_000_000_000L to "B"
        value >= 1_000_000L -> 1_000_000L to "M"
        value >= 1_000L -> 1_000L to "K"
        else -> return value.toString()
    }
    val whole = value / divisor
    if (whole >= 100) return "$whole$suffix"
    val tenth = value % divisor * 10 / divisor
    return if (tenth == 0L) "$whole$suffix" else "$whole.$tenth$suffix"
}
