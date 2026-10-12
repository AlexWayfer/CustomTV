package name.alexwayfer.customtv.ui.components

private const val THOUSAND = 1_000L
private const val MILLION = 1_000_000L
private const val BILLION = 1_000_000_000L

/**
 * A count shortened the way Twitch shows it, in English for every locale: `999`, `1.2K`, `12.1K`, `123K`, `4.5M`.
 * One decimal below 100 of a unit, none from 100; the decimal is cut, not rounded, so `11,999` stays `11.9K`
 * and never grows into the next unit.
 */
internal fun abbreviatedCount(value: Long): String = abbreviatedNumber(value, tenthBelow = 100L)

/** [value] in its largest whole unit, `K`, `M`, or `B`, with a cut tenth while the whole is below [tenthBelow]. */
internal fun abbreviatedNumber(value: Long, tenthBelow: Long): String {
    val (divisor, suffix) = when {
        value >= BILLION -> BILLION to "B"
        value >= MILLION -> MILLION to "M"
        value >= THOUSAND -> THOUSAND to "K"
        else -> return value.toString()
    }
    val whole = value / divisor
    val tenth = if (whole >= tenthBelow) 0L else value % divisor * 10 / divisor
    return if (tenth == 0L) "$whole$suffix" else "$whole.$tenth$suffix"
}
