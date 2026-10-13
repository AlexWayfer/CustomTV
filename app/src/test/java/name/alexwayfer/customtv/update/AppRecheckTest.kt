package name.alexwayfer.customtv.update

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRecheckTest {
    @Test
    fun noOpenCheckChecksAgain() {
        assertTrue(appRecheckDue(null))
    }

    @Test
    fun recentOpenCheckSkipsTheCheck() {
        assertFalse(appRecheckDue(30.seconds))
        assertFalse(appRecheckDue(29.minutes))
    }

    @Test
    fun openCheckAtTheIntervalChecksAgain() {
        assertTrue(appRecheckDue(30.minutes))
        assertTrue(appRecheckDue(45.minutes))
    }
}
