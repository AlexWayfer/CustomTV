package name.alexwayfer.customtv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInstallStepTest {
    @Test
    fun installWithoutThePermissionOpensTheSettings() {
        assertEquals(AppInstallStep.AllowInstalls, appInstallStep(canInstall = false))
    }

    @Test
    fun installWithThePermissionShowsTheHint() {
        assertEquals(AppInstallStep.PlayProtectHint, appInstallStep(canInstall = true))
    }

    @Test
    fun returningWithThePermissionShowsTheHint() {
        assertTrue(showsHintAfterSettings(canInstall = true))
    }

    @Test
    fun returningWithoutThePermissionShowsNothing() {
        assertFalse(showsHintAfterSettings(canInstall = false))
    }
}
