package name.alexwayfer.customtv

private const val MILLIS_PER_SECOND = 1_000L

/** [millis] in whole seconds, a second that has begun counted whole, as a countdown shows it. */
internal fun secondsRoundedUp(millis: Long): Long = (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
