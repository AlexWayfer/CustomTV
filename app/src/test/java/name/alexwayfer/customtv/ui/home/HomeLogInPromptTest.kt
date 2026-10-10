package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLogInPromptTest {
    @Test
    fun loggedOutAndNotPutAwayShowsTheOffer() {
        assertTrue(homeLogInPromptShown(signedIn = false, dismissed = false))
    }

    @Test
    fun loggedOutAfterNotNowHidesTheOffer() {
        assertFalse(homeLogInPromptShown(signedIn = false, dismissed = true))
    }

    @Test
    fun loggedOutBeforeTheAnswerIsReadHidesTheOffer() {
        assertFalse(homeLogInPromptShown(signedIn = false, dismissed = null))
    }

    @Test
    fun loggedInNeverShowsTheOffer() {
        assertFalse(homeLogInPromptShown(signedIn = true, dismissed = false))
    }
}
