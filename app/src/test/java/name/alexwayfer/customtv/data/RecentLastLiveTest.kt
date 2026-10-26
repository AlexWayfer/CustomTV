package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class RecentLastLiveTest {
    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun age(lastLive: String, now: String) = recentLastLive(millis(lastLive), millis(now), ZoneOffset.UTC)

    @Test
    fun aBroadcastThatEndedUnderAMinuteAgoIsJustNow() {
        assertEquals(RecentLastLive.JustNow, age("2026-09-24T14:59:01Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastEndingAheadOfTheClockIsJustNow() {
        assertEquals(RecentLastLive.JustNow, age("2026-09-24T15:05:00Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastUnderAnHourAgoCountsWholeMinutes() {
        assertEquals(RecentLastLive.MinutesAgo(1), age("2026-09-24T14:59:00Z", "2026-09-24T15:00:00Z"))
        assertEquals(RecentLastLive.MinutesAgo(59), age("2026-09-24T14:00:01Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastAnHourAgoCountsHours() {
        assertEquals(RecentLastLive.HoursAgo(1), age("2026-09-24T14:00:00Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastEarlierTodayUnderTwelveHoursAgoCountsHours() {
        assertEquals(RecentLastLive.HoursAgo(11), age("2026-09-24T03:00:01Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastEarlierTodayTwelveHoursAgoIsToday() {
        assertEquals(RecentLastLive.Today, age("2026-09-24T03:00:00Z", "2026-09-24T15:00:00Z"))
    }

    @Test
    fun aBroadcastLateYesterdayUnderTwelveHoursAgoCountsHours() {
        assertEquals(RecentLastLive.HoursAgo(2), age("2026-09-23T23:00:00Z", "2026-09-24T01:00:00Z"))
        assertEquals(RecentLastLive.HoursAgo(11), age("2026-09-23T13:00:01Z", "2026-09-24T01:00:00Z"))
    }

    @Test
    fun aBroadcastYesterdayTwelveHoursAgoIsYesterday() {
        assertEquals(RecentLastLive.Yesterday, age("2026-09-23T13:00:00Z", "2026-09-24T01:00:00Z"))
    }

    @Test
    fun aBroadcastTwoCalendarDaysAgoIsNotRecent() {
        assertNull(age("2026-09-22T23:59:00Z", "2026-09-24T00:01:00Z"))
    }
}
