package name.alexwayfer.customtv.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTabBackTest {
    @Test
    fun backOnAboutReturnsToHome() {
        assertEquals(PROFILE_TAB_FIRST, profileTabAfterBack(PROFILE_TAB_ABOUT))
    }

    @Test
    fun backOnTheFirstTabLeavesTheProfile() {
        assertNull(profileTabAfterBack(PROFILE_TAB_FIRST))
    }
}
