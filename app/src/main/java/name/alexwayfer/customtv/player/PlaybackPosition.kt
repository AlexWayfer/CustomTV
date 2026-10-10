package name.alexwayfer.customtv.player

/**
 * Where a recording plays now: the page reports its time a few times a second, and between reports a playing
 * recording moves on with the clock.
 */
internal fun playbackPositionMs(reportedMs: Long, reportedAtMs: Long, nowMs: Long, advancing: Boolean): Long {
    if (!advancing) return reportedMs
    return reportedMs + (nowMs - reportedAtMs).coerceAtLeast(0L)
}

/**
 * A recording's length for the progress bar, or null when it is unknown. The recording of a stream that is still
 * live grows past the length it had when the list loaded, so the bar never ends before the position.
 */
internal fun recordingDurationMs(durationSeconds: Long, positionMs: Long): Long? {
    val durationMs = durationSeconds.coerceAtLeast(0L) * 1_000L
    return maxOf(durationMs, positionMs).takeIf { it > 0L }
}
