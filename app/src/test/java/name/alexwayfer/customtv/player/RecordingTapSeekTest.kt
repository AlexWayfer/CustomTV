package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingTapSeekTest {
    @Test
    fun theOuterSharesOfTheWidthSeekAndTheMiddleDoesNot() {
        assertEquals(TapSide.Back, tapSide(x = 0f, width = 1000f))
        assertEquals(TapSide.Back, tapSide(x = 349f, width = 1000f))
        assertEquals(TapSide.Middle, tapSide(x = 350f, width = 1000f))
        assertEquals(TapSide.Middle, tapSide(x = 500f, width = 1000f))
        assertEquals(TapSide.Middle, tapSide(x = 650f, width = 1000f))
        assertEquals(TapSide.Forward, tapSide(x = 651f, width = 1000f))
        assertEquals(TapSide.Forward, tapSide(x = 1000f, width = 1000f))
    }

    @Test
    fun aVideoWithoutWidthHasOnlyTheMiddle() {
        assertEquals(TapSide.Middle, tapSide(x = 0f, width = 0f))
    }

    @Test
    fun aQuickSecondTapOnTheSameSideStartsSeeking() {
        assertTrue(seeks(TapSide.Forward, TapSide.Forward, sinceLastTapMs = 300))
        assertTrue(seeks(TapSide.Back, TapSide.Back, sinceLastTapMs = 0))
    }

    @Test
    fun aSlowSecondTapOrOneOnTheOtherSideDoesNotStartSeeking() {
        assertFalse(seeks(TapSide.Forward, TapSide.Forward, sinceLastTapMs = 301))
        assertFalse(seeks(TapSide.Back, TapSide.Forward, sinceLastTapMs = 100))
        assertFalse(seeks(null, TapSide.Forward, sinceLastTapMs = 100))
    }

    @Test
    fun whileASeriesRunsATapOnEitherSideSeeksEvenASlowOne() {
        assertTrue(seeks(TapSide.Forward, TapSide.Forward, sinceLastTapMs = 900, seriesRunning = true))
        assertTrue(seeks(TapSide.Forward, TapSide.Back, sinceLastTapMs = 900, seriesRunning = true))
    }

    @Test
    fun aTapInTheMiddleNeverSeeks() {
        assertFalse(seeks(TapSide.Middle, TapSide.Middle, sinceLastTapMs = 100))
        assertFalse(seeks(TapSide.Forward, TapSide.Middle, sinceLastTapMs = 100, seriesRunning = true))
    }

    /** [tapSeeks] with a double-tap timeout of 300 ms. */
    private fun seeks(previous: TapSide?, side: TapSide, sinceLastTapMs: Long, seriesRunning: Boolean = false) =
        tapSeeks(previous, side, sinceLastTapMs, doubleTapTimeoutMs = 300, seriesRunning = seriesRunning)

    @Test
    fun aFirstTapThatWouldHideTheControlsOnASideWaitsForASecond() {
        assertTrue(firstTapWaits(controlsVisible = true, side = TapSide.Back))
        assertTrue(firstTapWaits(controlsVisible = true, side = TapSide.Forward))
    }

    @Test
    fun aFirstTapThatShowsTheControlsOrLandsInTheMiddleActsAtOnce() {
        assertFalse(firstTapWaits(controlsVisible = false, side = TapSide.Forward))
        assertFalse(firstTapWaits(controlsVisible = true, side = TapSide.Middle))
    }

    @Test
    fun eachTapMovesTheSeriesTenSecondsItsWay() {
        assertEquals(10.0, tapSeekOffset(0.0, TapSide.Forward, startSeconds = 100.0, durationSeconds = 600), 0.0)
        assertEquals(30.0, tapSeekOffset(20.0, TapSide.Forward, startSeconds = 100.0, durationSeconds = 600), 0.0)
        assertEquals(-10.0, tapSeekOffset(0.0, TapSide.Back, startSeconds = 100.0, durationSeconds = 600), 0.0)
    }

    @Test
    fun aTapTheOtherWayTakesTenSecondsOffTheSeries() {
        assertEquals(20.0, tapSeekOffset(30.0, TapSide.Back, startSeconds = 100.0, durationSeconds = 600), 0.0)
        assertEquals(0.0, tapSeekOffset(-10.0, TapSide.Forward, startSeconds = 100.0, durationSeconds = 600), 0.0)
    }

    @Test
    fun theSeriesStaysWithinTheRecording() {
        assertEquals(-7.0, tapSeekOffset(0.0, TapSide.Back, startSeconds = 7.0, durationSeconds = 600), 0.0)
        assertEquals(5.0, tapSeekOffset(0.0, TapSide.Forward, startSeconds = 595.0, durationSeconds = 600), 0.0)
    }

    @Test
    fun anUnknownDurationBoundsOnlyTheStart() {
        assertEquals(10.0, tapSeekOffset(0.0, TapSide.Forward, startSeconds = 1_000.0, durationSeconds = 0), 0.0)
        assertEquals(-3.0, tapSeekOffset(0.0, TapSide.Back, startSeconds = 3.0, durationSeconds = 0), 0.0)
    }
}
