package name.alexwayfer.customtv.ui.watch

internal fun formatStreamUptime(startedAtMillis: Long, nowMillis: Long): String {
    val elapsedSeconds = ((nowMillis - startedAtMillis) / 1000L).coerceAtLeast(0L)
    val hours = elapsedSeconds / 3600L
    val minutes = (elapsedSeconds % 3600L) / 60L
    val seconds = elapsedSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
