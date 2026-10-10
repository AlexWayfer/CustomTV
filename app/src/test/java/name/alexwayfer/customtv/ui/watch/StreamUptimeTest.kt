package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUptimeTest {
    @Test
    fun formatsMinutesAndSecondsBeforeOneHour() {
        assertEquals("59:07", formatStreamUptime(startedAtMillis = 0L, nowMillis = 3_547_000L))
    }

    @Test
    fun formatsHoursMinutesAndSecondsFromOneHour() {
        assertEquals("1:02:03", formatStreamUptime(startedAtMillis = 0L, nowMillis = 3_723_000L))
    }

    @Test
    fun clampsFutureStartTimeToZero() {
        assertEquals("0:00", formatStreamUptime(startedAtMillis = 1_000L, nowMillis = 0L))
    }
}
