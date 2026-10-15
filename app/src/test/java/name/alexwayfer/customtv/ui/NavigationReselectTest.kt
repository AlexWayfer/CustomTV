package name.alexwayfer.customtv.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationReselectTest {
    @Test
    fun homeTappedAgainReselectsHome() {
        assertTrue(navigationTapReselects(open = AppSection.Home, tapped = AppSection.Home))
    }

    @Test
    fun settingsTappedAgainReselectsSettings() {
        assertTrue(navigationTapReselects(open = AppSection.Settings, tapped = AppSection.Settings))
    }

    @Test
    fun aTapFromAnotherSectionOnlyOpensIt() {
        assertFalse(navigationTapReselects(open = AppSection.Account, tapped = AppSection.Home))
        assertFalse(navigationTapReselects(open = AppSection.Settings, tapped = AppSection.Home))
        assertFalse(navigationTapReselects(open = AppSection.Home, tapped = AppSection.Settings))
    }
}
