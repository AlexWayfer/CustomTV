package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLeaveGuardTest {
    @Test
    fun aPageWithoutUnsavedChangesIsLeftAtOnce() {
        var left = false

        SettingsLeaveGuard().leave { left = true }

        assertTrue(left)
    }

    @Test
    fun aPageWithUnsavedChangesGetsTheLeaveInsteadOfLeaving() {
        val guard = SettingsLeaveGuard()
        var asked: (() -> Unit)? = null
        guard.onLeave = { asked = it }
        var left = 0

        guard.leave { left++ }

        assertEquals(0, left)
        asked!!()
        assertEquals(1, left)
    }
}
