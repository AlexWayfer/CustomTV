package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarAgeTest {
    private val zone = ZoneId.of("Europe/Moscow")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun countsCalendarYearsMonthsDaysHoursAndMinutes() {
        assertEquals(
            CalendarAge(5, 8, 15, 5, 20),
            calendarAge(millis(2021, 1, 10, 12), millis(2026, 9, 25, 17, 20), zone),
        )
    }

    @Test
    fun anHourEarlierInTheDayBorrowsFromTheDays() {
        assertEquals(CalendarAge(0, 0, 0, 23, 0), calendarAge(millis(2026, 10, 1, 18), millis(2026, 10, 2, 17), zone))
    }

    @Test
    fun aFutureTimeCountsAsNow() {
        assertEquals(CalendarAge(0, 0, 0, 0, 0), calendarAge(millis(2026, 10, 3, 0), millis(2026, 10, 2, 0), zone))
    }

    @Test
    fun aDayOrMoreNamesYearsDownToHoursWithoutZerosOrMinutes() {
        assertEquals(
            listOf(AgeUnit.Years to 2L, AgeUnit.Hours to 3L),
            CalendarAge(2, 0, 0, 3, 40).shownUnits(),
        )
        assertEquals(listOf(AgeUnit.Days to 1L), CalendarAge(0, 0, 1, 0, 59).shownUnits())
    }

    @Test
    fun lessThanADayNamesHoursAndMinutes() {
        assertEquals(listOf(AgeUnit.Hours to 5L, AgeUnit.Minutes to 7L), CalendarAge(0, 0, 0, 5, 7).shownUnits())
        assertEquals(listOf(AgeUnit.Hours to 5L), CalendarAge(0, 0, 0, 5, 0).shownUnits())
        assertEquals(listOf(AgeUnit.Minutes to 42L), CalendarAge(0, 0, 0, 0, 42).shownUnits())
    }

    @Test
    fun lessThanAMinuteShowsZeroMinutes() {
        assertEquals(listOf(AgeUnit.Minutes to 0L), CalendarAge(0, 0, 0, 0, 0).shownUnits())
    }

    @Test
    fun ownCardAlwaysReadsTheFollow() {
        assertEquals(ChatterFollowSource.Own, chatterFollowSource(ownCard = true, chatterUserId = null, moderatesChannel = false))
    }

    @Test
    fun someoneElseIsReadOnlyOnAChannelTheUserModerates() {
        assertEquals(ChatterFollowSource.Moderator, chatterFollowSource(ownCard = false, chatterUserId = "7", moderatesChannel = true))
        assertEquals(ChatterFollowSource.None, chatterFollowSource(ownCard = false, chatterUserId = "7", moderatesChannel = false))
        assertEquals(ChatterFollowSource.None, chatterFollowSource(ownCard = false, chatterUserId = null, moderatesChannel = true))
    }
}
