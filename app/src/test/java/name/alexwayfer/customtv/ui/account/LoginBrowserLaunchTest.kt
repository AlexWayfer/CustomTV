package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginBrowserLaunchTest {
    // Either the connection or the timeout comes first; whichever it is opens the page, and the other one does not.
    @Test
    fun firstOfConnectionAndTimeoutOpensThePageAndTheOtherOpensNothing() {
        val launch = LoginBrowserLaunch(generation = 1)
        assertTrue(launch.claim(currentGeneration = 1))
        assertFalse(launch.claim(currentGeneration = 1))
    }

    @Test
    fun aNewerLoginAttemptOutdatesBothOfTheOldOnes() {
        val launch = LoginBrowserLaunch(generation = 1)
        assertFalse(launch.claim(currentGeneration = 2))
        assertFalse(launch.claim(currentGeneration = 2))
    }
}
