package name.alexwayfer.customtv.player

import android.os.SystemClock

/** The page reports a recording's time in fractional seconds; a `Duration` would throw on a `NaN` from it. */
private const val MILLIS_PER_SECOND = 1_000.0

/** A recording's time as the page last reported it, and when, so a playing recording moves on between reports. */
internal class ReportedPosition {
    @Volatile
    private var reportedMs = 0L
    @Volatile
    private var reportedAt = 0L

    fun set(positionMs: Long) {
        reportedMs = positionMs.coerceAtLeast(0L)
        reportedAt = SystemClock.elapsedRealtime()
    }

    fun setSeconds(seconds: Double) {
        set((seconds * MILLIS_PER_SECOND).toLong())
    }

    fun currentMs(advancing: Boolean): Long = playbackPositionMs(
        reportedMs = reportedMs,
        reportedAtMs = reportedAt,
        nowMs = SystemClock.elapsedRealtime(),
        advancing = advancing,
    )

    companion object {
        fun seconds(positionMs: Long): Double = positionMs / MILLIS_PER_SECOND
    }
}
