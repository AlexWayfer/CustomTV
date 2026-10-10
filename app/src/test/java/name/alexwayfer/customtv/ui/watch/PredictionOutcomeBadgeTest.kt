package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictionOutcomeBadgeTest {
    @Test
    fun twoOutcomesAreBlueAndPink() {
        assertEquals(PredictionOutcomeColor.Blue, predictionOutcomeColor(index = 0, count = 2))
        assertEquals(PredictionOutcomeColor.Pink, predictionOutcomeColor(index = 1, count = 2))
    }

    @Test
    fun moreOutcomesAreAllBlue() {
        assertEquals(PredictionOutcomeColor.Blue, predictionOutcomeColor(index = 1, count = 3))
        assertEquals(PredictionOutcomeColor.Blue, predictionOutcomeColor(index = 2, count = 3))
    }
}
