package name.alexwayfer.customtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePlayerPageTest {
    @Test
    fun anOfflinePageIsSuspendedWhenTheAppLeavesTheScreen() {
        assertTrue(suspendOfflinePlayerPage(background = true, offline = true))
    }

    @Test
    fun anOfflinePageCanWaitForTheStreamOnScreen() {
        assertFalse(suspendOfflinePlayerPage(background = false, offline = true))
    }

    @Test
    fun anAlreadyPlayingOrLoadingPageStaysInTheBackground() {
        assertFalse(suspendOfflinePlayerPage(background = true, offline = false))
    }

    @Test
    fun aSuspendedPageStaysSuspendedUntilTheAppReturns() {
        assertTrue(suspendOfflinePlayerPage(background = true, offline = false, suspended = true))
        assertFalse(suspendOfflinePlayerPage(background = false, offline = false, suspended = true))
    }

    @Test
    fun anOfflineReportAfterMinimizingUnloadsOnceAndReturningReloadsOnce() {
        val actions = mutableListOf<String>()
        val page = OfflinePlayerPage(unload = { actions += "unload" }, reload = { actions += "reload" })
        page.update(background = true, offline = false)
        assertTrue(actions.isEmpty())

        page.update(offline = true)
        page.update(background = true, offline = true)
        page.update(background = true, offline = false)
        assertTrue(page.suspended)
        assertEquals(listOf("unload"), actions)

        page.update(background = false, offline = false)
        page.update(background = false, offline = false)
        assertFalse(page.suspended)
        assertEquals(listOf("unload", "reload"), actions)
    }
}
