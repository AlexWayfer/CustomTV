package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppLinksPromptTest {
    @Test
    fun theCardShowsWhileTheLinksNeedSettingUpAndNothingAboveItWaits() {
        assertTrue(homeAppLinksPromptVisible(needSetup = true, enabled = false, dismissed = false, false))
    }

    @Test
    fun theCardWaitsForTheCardsAboveIt() {
        assertFalse(homeAppLinksPromptVisible(needSetup = true, enabled = false, dismissed = false, true))
    }

    @Test
    fun theCardStaysAwayAfterNotNowOrBeforeItsAnswerIsRead() {
        assertFalse(homeAppLinksPromptVisible(needSetup = true, enabled = false, dismissed = true, false))
        assertFalse(homeAppLinksPromptVisible(needSetup = true, enabled = false, dismissed = null, false))
    }

    @Test
    fun theCardStaysAwayOnceTheLinksAreOnOrOnOlderAndroid() {
        assertFalse(homeAppLinksPromptVisible(needSetup = true, enabled = true, dismissed = false, false))
        assertFalse(homeAppLinksPromptVisible(needSetup = false, enabled = false, dismissed = false, false))
    }

    @Test
    fun aCardAboveIsPendingWhileShownOrBeforeItsAnswerIsRead() {
        assertTrue(homePromptPending(dismissed = null, visible = false))
        assertTrue(homePromptPending(dismissed = false, visible = true))
        assertFalse(homePromptPending(dismissed = false, visible = false))
        assertFalse(homePromptPending(dismissed = true, visible = false))
    }
}
