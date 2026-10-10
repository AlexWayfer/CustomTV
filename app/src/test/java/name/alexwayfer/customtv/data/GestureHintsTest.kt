package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureHintsTest {
    @Test
    fun slowUsesBelowThresholdCountWithoutShowing() {
        val step = gestureHintAfterSlowUse(GestureHintProgress(slowUses = 1), threshold = 3)

        assertEquals(GestureHintSlowUse(GestureHintProgress(slowUses = 2), show = false), step)
    }

    @Test
    fun slowUseReachingThresholdShowsAndMarksShown() {
        val step = gestureHintAfterSlowUse(GestureHintProgress(slowUses = 2), threshold = 3)

        assertEquals(GestureHintSlowUse(GestureHintProgress(slowUses = 3, shown = true), show = true), step)
    }

    @Test
    fun firstSlowUseShowsWithThresholdOne() {
        val step = gestureHintAfterSlowUse(GestureHintProgress(), threshold = 1)

        assertTrue(step.show)
    }

    @Test
    fun shownTipNeverShowsAgainAndStopsCounting() {
        val shown = GestureHintProgress(slowUses = 3, shown = true)

        assertEquals(GestureHintSlowUse(shown, show = false), gestureHintAfterSlowUse(shown, threshold = 3))
    }

    @Test
    fun usedGestureNeverShowsTipAndStopsCounting() {
        val used = GestureHintProgress(gestureUsed = true, slowUses = 2)

        assertEquals(GestureHintSlowUse(used, show = false), gestureHintAfterSlowUse(used, threshold = 3))
    }
}
