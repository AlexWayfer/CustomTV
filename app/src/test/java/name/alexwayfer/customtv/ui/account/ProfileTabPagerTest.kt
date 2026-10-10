package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTabPagerTest {
    @Test
    fun aSwipeThatRestsOnATabShowsIt() {
        assertEquals(ProfilePageSettled.ShowTab(PROFILE_TAB_ABOUT), profilePageSettled(PROFILE_TAB_ABOUT, tabCount = 3))
    }

    @Test
    fun aSwipeThatRestsOnTheLastTabShowsIt() {
        assertEquals(ProfilePageSettled.ShowTab(2), profilePageSettled(2, tabCount = 3))
    }

    @Test
    fun aSwipePastTheLastTabOpensChat() {
        assertEquals(ProfilePageSettled.OpenChat, profilePageSettled(3, tabCount = 3))
    }

    @Test
    fun shortTabsGrowToTheBottomOfTheScreen() {
        assertEquals(1500, profileTabsMinHeightPx(viewportPx = 2000, contentPx = 900, tabsPx = 400))
    }

    @Test
    fun theLeastHeightStaysOnceTheTabsHaveGrown() {
        assertEquals(1500, profileTabsMinHeightPx(viewportPx = 2000, contentPx = 2000, tabsPx = 1500))
    }

    @Test
    fun aHeaderTallerThanTheScreenAsksForNoHeight() {
        assertEquals(0, profileTabsMinHeightPx(viewportPx = 1000, contentPx = 1600, tabsPx = 400))
    }

    @Test
    fun aSwipePastAboutOpensChatWhenVideosAreMissing() {
        assertEquals(ProfilePageSettled.OpenChat, profilePageSettled(2, tabCount = 2))
    }
}
