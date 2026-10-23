package name.alexwayfer.customtv.update

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class AppUpdateAfterProblemTest {
    private val version = AppVersion.parse("9.9")!!

    @Test
    fun aProblemWithoutARecentCheckChecksForAnUpdate() {
        assertEquals(AppUpdateAfterProblem.Check, appUpdateAfterProblem(sinceCheck = null, known = version))
        assertEquals(AppUpdateAfterProblem.Check, appUpdateAfterProblem(APP_RECHECK_INTERVAL, known = null))
    }

    @Test
    fun aProblemAfterARecentCheckShowsTheUpdateItFound() {
        assertEquals(AppUpdateAfterProblem.Show(version), appUpdateAfterProblem(1.minutes, known = version))
    }

    @Test
    fun aProblemAfterARecentCheckWithoutAnUpdateLeavesTheNotices() {
        assertEquals(AppUpdateAfterProblem.None, appUpdateAfterProblem(1.minutes, known = null))
    }
}
