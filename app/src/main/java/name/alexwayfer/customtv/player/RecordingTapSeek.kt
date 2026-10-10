package name.alexwayfer.customtv.player

import android.os.Handler
import kotlin.math.roundToInt

internal enum class TapSide { Back, Middle, Forward }

/** A running series of seeking taps: how far it moved the recording in all, back below zero. */
internal data class TapSeekSeries(val seconds: Int)

/** Where a tap lands across the video: the outer [TAP_SEEK_SIDE_SHARE] on either side seeks, the middle does not. */
internal fun tapSide(x: Float, width: Float): TapSide = when {
    width <= 0f -> TapSide.Middle
    x < width * TAP_SEEK_SIDE_SHARE -> TapSide.Back
    x > width * (1f - TAP_SEEK_SIDE_SHARE) -> TapSide.Forward
    else -> TapSide.Middle
}

/**
 * Whether a tap on a side seeks: a quick second tap on the same side starts a series, and while one runs every tap on
 * either side goes on with it. A tap in the middle never seeks.
 */
internal fun tapSeeks(
    previous: TapSide?,
    side: TapSide,
    sinceLastTapMs: Long,
    doubleTapTimeoutMs: Long,
    seriesRunning: Boolean,
): Boolean = side != TapSide.Middle &&
    (seriesRunning || side == previous && sinceLastTapMs in 0..doubleTapTimeoutMs)

/**
 * A first tap that would hide the controls waits for a second one on a side, which seeks with the controls up instead.
 * A tap that shows them, or one in the middle, acts at once.
 */
internal fun firstTapWaits(controlsVisible: Boolean, side: TapSide): Boolean = controlsVisible && side != TapSide.Middle

/**
 * The series' move after one more tap on [side]: [TAP_SEEK_STEP_SECONDS] back or forward from [offsetSeconds], counted
 * from [startSeconds], where the series began. The place it leads to stays within the recording; a [durationSeconds]
 * of 0 is unknown and bounds only the start.
 */
internal fun tapSeekOffset(offsetSeconds: Double, side: TapSide, startSeconds: Double, durationSeconds: Long): Double {
    val direction = when (side) {
        TapSide.Back -> -1
        TapSide.Middle -> 0
        TapSide.Forward -> 1
    }
    val offset = (offsetSeconds + direction * TAP_SEEK_STEP_SECONDS).coerceAtLeast(-startSeconds)
    return if (durationSeconds > 0) offset.coerceAtMost(durationSeconds - startSeconds) else offset
}

/**
 * Taps on a recording's video: a single one shows or hides the controls through [chromeTap], a quick second one on a
 * side starts seeking back or forward with the controls up. While the series shows, taps on either side change it.
 */
internal class RecordingTapSeek(
    private val handler: Handler,
    private val doubleTapTimeoutMs: Long,
    private val now: () -> Long,
    private val positionSeconds: () -> Double,
    private val durationSeconds: () -> Long,
    private val controlsVisible: () -> Boolean,
    private val seek: (seconds: Double) -> Unit,
    /** A tap that toggles the chrome and the controls, given whether the controls show now. */
    private val chromeTap: (controlsVisible: Boolean) -> Unit,
    /** The series after each seeking tap, and null once it ends. */
    private val onSeries: (TapSeekSeries?) -> Unit,
) {
    private var lastSide: TapSide? = null
    private var lastTapAt = 0L
    private var seriesRunning = false
    private var seriesStartSeconds = 0.0
    private var seriesOffsetSeconds = 0.0
    private val pendingHide = Runnable { chromeTap(controlsVisible()) }
    private val seriesEnd = Runnable { endSeries() }

    fun onTap(x: Float, width: Float) {
        val side = tapSide(x, width)
        val at = now()
        val seeks = tapSeeks(lastSide, side, at - lastTapAt, doubleTapTimeoutMs, seriesRunning)
        lastSide = side
        lastTapAt = at
        handler.removeCallbacks(pendingHide)
        if (seeks) {
            if (!seriesRunning) {
                seriesRunning = true
                seriesStartSeconds = positionSeconds()
                seriesOffsetSeconds = 0.0
            }
            seriesOffsetSeconds = tapSeekOffset(seriesOffsetSeconds, side, seriesStartSeconds, durationSeconds())
            seek(seriesStartSeconds + seriesOffsetSeconds)
            onSeries(TapSeekSeries(seriesOffsetSeconds.roundToInt()))
            handler.removeCallbacks(seriesEnd)
            handler.postDelayed(seriesEnd, TAP_SEEK_HOLD_MS)
            // The controls stay up with the time they show; reported hidden, the tap only brings them up.
            if (!controlsVisible()) chromeTap(false)
            return
        }
        // A tap in the middle ends the series at once.
        endSeries()
        val visible = controlsVisible()
        if (firstTapWaits(visible, side)) {
            handler.postDelayed(pendingHide, doubleTapTimeoutMs)
        } else {
            chromeTap(visible)
        }
    }

    fun cancel() {
        handler.removeCallbacks(pendingHide)
        endSeries()
        lastSide = null
    }

    private fun endSeries() {
        handler.removeCallbacks(seriesEnd)
        if (!seriesRunning) return
        seriesRunning = false
        onSeries(null)
    }
}

/** The share of the video's width on either side where taps seek. */
internal const val TAP_SEEK_SIDE_SHARE = 0.35f

/** How far each seeking tap moves a recording. */
internal const val TAP_SEEK_STEP_SECONDS = 10

/** How long a series stays on screen after its last tap, taking more taps on either side. */
internal const val TAP_SEEK_HOLD_MS = 1_000L
