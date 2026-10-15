package name.alexwayfer.customtv.ui.home

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
}
