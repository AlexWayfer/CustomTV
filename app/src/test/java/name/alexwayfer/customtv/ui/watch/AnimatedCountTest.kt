package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedCountTest {
    @Test
    fun theCountStartsAtTheShownNumberAndEndsExactlyOnTheTarget() {
        assertEquals(100L, countBetween(100L, 200L, 0f))
        assertEquals(150L, countBetween(100L, 200L, 0.5f))
        assertEquals(200L, countBetween(100L, 200L, 1f))
    }

    @Test
    fun aFallingCountRunsDown() {
        assertEquals(75L, countBetween(100L, 50L, 0.5f))
    }

    @Test
    fun aTotalBeyondFloatPrecisionEndsOnItsExactValue() {
        assertEquals(123_456_789L, countBetween(0L, 123_456_789L, 1f))
        assertEquals(3_000_000_001L, countBetween(3_000_000_000L, 3_000_000_001L, 1f))
    }
}
