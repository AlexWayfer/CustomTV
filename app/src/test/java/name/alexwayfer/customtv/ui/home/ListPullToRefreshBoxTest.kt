package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ListPullToRefreshBoxTest {
    @Test
    fun indicatorAtTheThresholdCentersBelowTheGap() {
        assertEquals(28f, pullIndicatorCenter(distanceFraction = 1f, maxDistancePx = 48f, heightPx = 40f), 0.001f)
    }

    @Test
    fun indicatorPulledPastTheThresholdCentersLower() {
        assertEquals(40f, pullIndicatorCenter(distanceFraction = 1.25f, maxDistancePx = 48f, heightPx = 40f), 0.001f)
    }

    @Test
    fun releasedIndicatorStaysOpaqueForMostOfItsFlow() {
        assertEquals(1f, pullIndicatorFlowAlpha(0f), 0.001f)
        assertEquals(1f, pullIndicatorFlowAlpha(0.6f), 0.001f)
    }

    @Test
    fun releasedIndicatorFadesOutAtTheEndOfItsFlow() {
        assertEquals(0.5f, pullIndicatorFlowAlpha(0.8f), 0.001f)
        assertEquals(0f, pullIndicatorFlowAlpha(1f), 0.001f)
    }

    @Test
    fun barUnderATitleCentersInTheBandAlongItsBottom() {
        assertEquals(54, listLoadingBarTop(headerHeightPx = 60, bandPx = 8, barPx = 4))
    }

    @Test
    fun barWithoutATitleRunsAlongTheTopEdge() {
        assertEquals(0, listLoadingBarTop(headerHeightPx = 0, bandPx = 8, barPx = 4))
    }

    @Test
    fun barNeverGoesAboveTheTopOfATitleShorterThanTheBand() {
        assertEquals(0, listLoadingBarTop(headerHeightPx = 3, bandPx = 8, barPx = 4))
    }
}
