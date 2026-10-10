package name.alexwayfer.customtv.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumBannerBackgroundTest {
    @Test
    fun aSquareScreenFadesBothGlowsOutAtItsCenter() {
        val glows = premiumBannerGlows(100f, 100f)

        assertEquals(Offset(100f, 0f), glows.purpleFrom)
        assertEquals(Offset(50f, 50f), glows.purpleTo)
        assertEquals(Offset(0f, 100f), glows.goldFrom)
        assertEquals(Offset(50f, 50f), glows.goldTo)
    }

    @Test
    fun aTallScreenFadesEachGlowOutOnTheDiagonalNearestItsCorner() {
        val glows = premiumBannerGlows(30f, 40f)

        // The diagonal runs along (3, 4); the top right corner meets it at 9/25 of it, the bottom left at 16/25.
        assertEquals(10.8f, glows.purpleTo.x, 0.001f)
        assertEquals(14.4f, glows.purpleTo.y, 0.001f)
        assertEquals(19.2f, glows.goldTo.x, 0.001f)
        assertEquals(25.6f, glows.goldTo.y, 0.001f)
    }

    @Test
    fun aZeroSizeStaysAtTheOrigin() {
        val glows = premiumBannerGlows(0f, 0f)

        assertEquals(Offset.Zero, glows.purpleTo)
        assertEquals(Offset.Zero, glows.goldTo)
    }

    @Test
    fun aGlowKeepsItsWayWithoutTurning() {
        assertEquals(Offset(6f, 8f), premiumBannerGlowEnd(Offset.Zero, Offset(6f, 8f), reach = 1f, sideways = 1f))
    }

    @Test
    fun aTurnedGlowGoesMoreDownAndAsFar() {
        val end = premiumBannerGlowEnd(Offset.Zero, Offset(10f, 10f), reach = 0.5f, sideways = 0f)

        assertEquals(0f, end.x, 0.001f)
        assertEquals(7.071f, end.y, 0.001f)
    }

    @Test
    fun aGlowWithNowhereToGoStaysAtItsCorner() {
        val corner = Offset(5f, 5f)

        assertEquals(corner, premiumBannerGlowEnd(corner, corner, reach = 0.6f, sideways = 0.6f))
    }
}
