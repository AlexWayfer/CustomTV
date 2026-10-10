package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPageVisibilityTest {
    private fun loadedPage() = LoginPageVisibility().apply {
        shown(now = 1_000L)
        loaded()
    }

    @Test
    fun hidingWhileTheAppIsInFrontClosesTheTab() {
        assertTrue(loadedPage().hidden(now = 5_000L, appInFront = true))
    }

    @Test
    fun switchingToAnotherAppKeepsTheLogin() {
        val page = loadedPage()

        assertFalse(page.hidden(now = 5_000L, appInFront = false))
    }

    @Test
    fun appComingToFrontAfterTheTabHidClosesTheTab() {
        val page = loadedPage()
        page.hidden(now = 5_000L, appInFront = false)

        assertTrue(page.appResumed())
    }

    @Test
    fun returningToTheTabFromAnotherAppKeepsTheLogin() {
        val page = loadedPage()
        page.hidden(now = 5_000L, appInFront = false)
        page.shown(now = 9_000L)

        assertFalse(page.appResumed())
    }

    @Test
    fun appResumingWhileTheTabIsShownKeepsTheLogin() {
        assertFalse(loadedPage().appResumed())
    }

    @Test
    fun hideBeforeTheTabSettlesIsIgnored() {
        val page = LoginPageVisibility().apply { shown(now = 1_000L) }

        assertFalse(page.hidden(now = 1_500L, appInFront = true))
        assertFalse(page.appResumed())
    }

    @Test
    fun hideASecondAfterShowingCountsWithoutALoadedPage() {
        val page = LoginPageVisibility().apply { shown(now = 1_000L) }

        assertTrue(page.hidden(now = 2_000L, appInFront = true))
    }

    @Test
    fun closedTabReportsOnlyOnce() {
        val page = loadedPage()

        assertTrue(page.hidden(now = 5_000L, appInFront = true))
        assertFalse(page.appResumed())
        assertFalse(page.hidden(now = 6_000L, appInFront = true))
    }
}
