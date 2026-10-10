package name.alexwayfer.customtv.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumBannerScheduleTest {
    private val now = 1_000_000_000L
    private val interval = PREMIUM_BANNER_INTERVAL.inWholeMilliseconds
    private val opens = PREMIUM_BANNER_PLAYER_OPENS
    private val shown = now

    @Test
    fun theFirstBannerWaitsForEnoughOpens() {
        assertFalse(premiumBannerDue(now, null, opens - 1))
    }

    @Test
    fun theFirstBannerComesRightAfterEnoughOpens() {
        assertTrue(premiumBannerDue(now, null, opens))
    }

    @Test
    fun aShownBannerWaitsForTheInterval() {
        assertFalse(premiumBannerDue(shown + interval - 1, shown, opens))
    }

    @Test
    fun aShownBannerWaitsForEnoughOpensAfterTheInterval() {
        assertFalse(premiumBannerDue(shown + interval * 2, shown, opens - 1))
    }

    @Test
    fun aShownBannerComesBackAfterTheIntervalAndEnoughOpens() {
        assertTrue(premiumBannerDue(shown + interval, shown, opens))
    }

    @Test
    fun aClockMovedBehindTheLastShowCountsAsTheIntervalPassed() {
        assertTrue(premiumBannerDue(shown - 1, shown, opens))
    }

    @Test
    fun aPlayerAppearingIsAnOpen() {
        assertTrue(premiumBannerPlayerOpened(wasPlayerOpen = false, playerOpen = true))
    }

    @Test
    fun aPlayerAlreadyOpenIsNotANewOpen() {
        assertFalse(premiumBannerPlayerOpened(wasPlayerOpen = true, playerOpen = true))
    }

    @Test
    fun aClosedPlayerBackInTheAppIsTheMoment() {
        assertTrue(premiumBannerMoment(wasPlayerOpen = true, playerOpen = false, inPictureInPicture = false))
    }

    @Test
    fun aPlayerStillOpenIsNotTheMoment() {
        assertFalse(premiumBannerMoment(wasPlayerOpen = true, playerOpen = true, inPictureInPicture = false))
    }

    @Test
    fun noPlayerBeforeIsNotTheMoment() {
        assertFalse(premiumBannerMoment(wasPlayerOpen = false, playerOpen = false, inPictureInPicture = false))
    }

    @Test
    fun aPlayerClosedInPictureInPictureIsNotTheMoment() {
        assertFalse(premiumBannerMoment(wasPlayerOpen = true, playerOpen = false, inPictureInPicture = true))
    }

    @Test
    fun comingBackToTheHomeScreenIsTheMoment() {
        assertTrue(premiumBannerReturnMoment(playerOpen = false, inPictureInPicture = false, onHome = true))
    }

    @Test
    fun comingBackToAnotherSectionIsNotTheMoment() {
        assertFalse(premiumBannerReturnMoment(playerOpen = false, inPictureInPicture = false, onHome = false))
    }

    @Test
    fun comingBackToAPlayerIsNotTheMoment() {
        assertFalse(premiumBannerReturnMoment(playerOpen = true, inPictureInPicture = false, onHome = true))
        assertFalse(premiumBannerReturnMoment(playerOpen = false, inPictureInPicture = true, onHome = true))
    }
}
