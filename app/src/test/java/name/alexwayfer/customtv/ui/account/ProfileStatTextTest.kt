package name.alexwayfer.customtv.ui.account

import name.alexwayfer.customtv.data.RecentLastLive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ProfileStatTextTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val now = ZonedDateTime.of(2026, 10, 5, 0, 30, 0, 0, zone).toInstant().toEpochMilli()

    private fun daysBefore(days: Long, hour: Int = 23) =
        ZonedDateTime.of(2026, 10, 5, hour, 0, 0, 0, zone).minusDays(days).toInstant().toEpochMilli()

    @Test
    fun aStreamThatEndedHalfAnHourAgoCountsMinutes() {
        assertEquals(LastLiveDay.Recent(RecentLastLive.MinutesAgo(30)), lastLiveDay(daysBefore(0, hour = 0), now, zone))
    }

    @Test
    fun aStreamFromLastNightCountsHours() {
        assertEquals(LastLiveDay.Recent(RecentLastLive.HoursAgo(1)), lastLiveDay(daysBefore(1), now, zone))
    }

    @Test
    fun aStreamFromYesterdayMorningIsYesterday() {
        assertEquals(LastLiveDay.Recent(RecentLastLive.Yesterday), lastLiveDay(daysBefore(1, hour = 10), now, zone))
    }

    @Test
    fun aStreamFromThreeDaysAgoCountsDays() {
        assertEquals(LastLiveDay.DaysAgo(3), lastLiveDay(daysBefore(3), now, zone))
    }

    @Test
    fun aWeekAgoStillCountsDays() {
        assertEquals(LastLiveDay.DaysAgo(7), lastLiveDay(daysBefore(7), now, zone))
    }

    @Test
    fun moreThanAWeekAgoShowsTheDate() {
        assertEquals(LastLiveDay.OnDate, lastLiveDay(daysBefore(8), now, zone))
    }

    @Test
    fun anEndAheadOfTheClockIsJustNow() {
        assertEquals(LastLiveDay.Recent(RecentLastLive.JustNow), lastLiveDay(now + 3_600_000, now, zone))
    }

    @Test
    fun theCountAtTheStartIsFound() {
        assertEquals(0 until 5, profileStatValueRange("1,234 followers", "1,234"))
    }

    @Test
    fun theDateAtTheEndIsFound() {
        assertEquals(10 until 21, profileStatValueRange("Last live Aug 8, 2023", "Aug 8, 2023"))
    }

    @Test
    fun aValueTheTextLeftOutIsNotMarked() {
        assertNull(profileStatValueRange("One follower", "1"))
    }

    @Test
    fun anEmptyValueIsNotMarked() {
        assertNull(profileStatValueRange("followers", ""))
    }

    @Test
    fun aDateWrapsWholeWhileTheWordsBeforeItStillBreak() {
        val text = "Последний эфир 15 апр. 2026 г."

        assertEquals(
            "Последний эфир 15\u00A0апр.\u00A02026\u00A0г.",
            profileStatUnbrokenValue(text, profileStatValueRange(text, "15 апр. 2026 г.")),
        )
    }

    @Test
    fun aTextWithoutTheValueStaysAsItWas() {
        assertEquals("One follower", profileStatUnbrokenValue("One follower", null))
    }
}
