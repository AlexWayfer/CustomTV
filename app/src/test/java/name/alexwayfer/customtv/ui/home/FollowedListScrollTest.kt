package name.alexwayfer.customtv.ui.home

import name.alexwayfer.customtv.ui.AppSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedListScrollTest {
    @Test
    fun theTopOfTheListStaysAtTheTop() {
        assertTrue(followedListKeepsTop(firstVisibleIndex = 0, scrollOffset = 0))
    }

    @Test
    fun aScrolledListDoesNotJumpBack() {
        assertFalse(followedListKeepsTop(firstVisibleIndex = 4, scrollOffset = 0))
        assertFalse(followedListKeepsTop(firstVisibleIndex = 0, scrollOffset = 12))
    }

    @Test
    fun homeTappedAgainScrollsTheListToTheTop() {
        assertTrue(homeTapScrollsListToTop(AppSection.Home))
    }

    @Test
    fun homeTappedFromAnotherSectionOnlyOpensHome() {
        assertFalse(homeTapScrollsListToTop(AppSection.Account))
        assertFalse(homeTapScrollsListToTop(AppSection.Settings))
    }
}
