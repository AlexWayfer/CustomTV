package name.alexwayfer.customtv.ui

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressMinShownTest {
    @Test
    fun progressShownBrieflyStaysForTheRestOfTheMinimum() {
        assertEquals(450.milliseconds, progressHideDelay(50.milliseconds))
    }

    @Test
    fun progressShownForTheMinimumHidesAtOnce() {
        assertEquals(Duration.ZERO, progressHideDelay(500.milliseconds))
    }

    @Test
    fun progressShownLongerThanTheMinimumHidesAtOnce() {
        assertEquals(Duration.ZERO, progressHideDelay(2_000.milliseconds))
    }
}
