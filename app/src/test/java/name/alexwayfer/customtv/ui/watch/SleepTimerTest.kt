package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SleepTimerTest {
    @Test
    fun zeroHoursAndMinutesStartNothing() {
        assertNull(sleepTimerDuration(SleepTimerPick(hours = 0, minutes = 0)))
    }

    @Test
    fun hoursAndMinutesAddUp() {
        assertEquals(90.minutes, sleepTimerDuration(SleepTimerPick(hours = 1, minutes = 30))!!)
    }

    @Test
    fun aTimerPastItsEndHasNothingLeft() {
        assertEquals(Duration.ZERO, sleepTimerLeft(endsAtMillis = 1_000L, nowMillis = 5_000L))
    }

    @Test
    fun theTimeLeftCountsToTheEnd() {
        assertEquals(4.seconds, sleepTimerLeft(endsAtMillis = 5_000L, nowMillis = 1_000L))
    }

    @Test
    fun aRunningTimerOpensThePickerOnTheTimeLeftRoundedUpToFiveMinutes() {
        assertEquals(
            SleepTimerPick(hours = 1, minutes = 25),
            sleepTimerPickerStart(left = 1.hours + 20.minutes + 1.seconds, lastSet = 2.hours),
        )
    }

    @Test
    fun aMinuteLeftRoundsUpToTheFirstStep() {
        assertEquals(SleepTimerPick(hours = 0, minutes = 5), sleepTimerPickerStart(left = 1.minutes, lastSet = null))
    }

    @Test
    fun aTimeLeftOnAStepIsNotRoundedFurther() {
        assertEquals(SleepTimerPick(hours = 0, minutes = 5), sleepTimerPickerStart(left = 5.minutes, lastSet = null))
    }

    @Test
    fun fiftyFiveMinutesAndASecondRoundUpToTheNextHour() {
        assertEquals(
            SleepTimerPick(hours = 1, minutes = 0),
            sleepTimerPickerStart(left = 55.minutes + 1.seconds, lastSet = null),
        )
    }

    @Test
    fun anIdleTimerOpensThePickerOnTheTimeSetLast() {
        assertEquals(SleepTimerPick(hours = 0, minutes = 45), sleepTimerPickerStart(left = null, lastSet = 45.minutes))
    }

    @Test
    fun theFirstPickStartsAtZero() {
        assertEquals(SleepTimerPick(hours = 0, minutes = 0), sleepTimerPickerStart(left = null, lastSet = null))
    }

    @Test
    fun thePickerStopsAtItsLastHourAndMinute() {
        assertEquals(SleepTimerPick(hours = 23, minutes = 55), sleepTimerPickerStart(left = 30.hours, lastSet = null))
    }

    @Test
    fun theWarningComesAMinuteBeforeTheEnd() {
        assertEquals(29.minutes, sleepTimerWarningDelay(30.minutes))
    }

    @Test
    fun aTimerWithLessThanAMinuteLeftWarnsAtOnce() {
        assertEquals(Duration.ZERO, sleepTimerWarningDelay(40.seconds))
    }

    @Test
    fun theTimeLeftShowsHoursMinutesAndSeconds() {
        assertEquals("1:02:03", formatSleepTimerLeft(1.hours + 2.minutes + 3.seconds))
    }

    @Test
    fun theWheelStartsOnItsValueAndLoopsBothWays() {
        val start = wheelStartIndex(value = 7, count = 24)

        assertEquals(7, wheelValue(start, count = 24))
        assertEquals(6, wheelValue(start - 1, count = 24))
        assertEquals(0, wheelValue(start - 7, count = 24))
        assertEquals(23, wheelValue(start - 8, count = 24))
    }
}
