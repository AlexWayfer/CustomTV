package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginClickTest {
    @Test
    fun aLoginClickStartsWhenNoneIsRunning() {
        assertTrue(loginClickStarts(loggingIn = false))
    }

    @Test
    fun aSecondLoginClickDoesNotStartAnotherBrowser() {
        assertFalse(loginClickStarts(loggingIn = true))
    }

    @Test
    fun backFromAStreamLoginReturnsToTheFullPlayer() {
        assertTrue(playerExpandsAfterLoginCancelled(openedFromStream = true, watching = true))
    }

    @Test
    fun backFromANavbarLoginLeavesThePlayerAsItWas() {
        assertFalse(playerExpandsAfterLoginCancelled(openedFromStream = false, watching = true))
        assertFalse(playerExpandsAfterLoginCancelled(openedFromStream = true, watching = false))
    }
}
