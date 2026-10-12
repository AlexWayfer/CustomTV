package name.alexwayfer.customtv

import org.junit.Assert.assertEquals
import org.junit.Test

class SecondsRoundedUpTest {
    @Test
    fun aBegunSecondCountsWhole() {
        assertEquals(0L, secondsRoundedUp(0))
        assertEquals(1L, secondsRoundedUp(1))
        assertEquals(1L, secondsRoundedUp(1_000))
        assertEquals(2L, secondsRoundedUp(1_001))
    }

    @Test
    fun lessThanASecondPastIsNone() {
        assertEquals(0L, secondsRoundedUp(-500))
    }
}
