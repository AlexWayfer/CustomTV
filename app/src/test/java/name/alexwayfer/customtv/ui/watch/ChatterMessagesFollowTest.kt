package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatterMessagesFollowTest {
    @Test
    fun newMessageKeepsFollowingWhenReaderWasAtTheEnd() {
        assertTrue(followChatterMessagesEnd(following = true, previousMax = 300, value = 300, max = 360))
    }

    @Test
    fun newMessageDoesNotPullReaderWhoScrolledUp() {
        assertFalse(followChatterMessagesEnd(following = false, previousMax = 300, value = 120, max = 360))
    }

    @Test
    fun scrollingUpStopsFollowing() {
        assertFalse(followChatterMessagesEnd(following = true, previousMax = 360, value = 200, max = 360))
    }

    @Test
    fun dragThatBeganInsideTheListStopsAtItsTop() {
        assertTrue(sheetContentKeepsDrag(startedAtTop = false, contentMoved = false, availableY = 40f))
    }

    @Test
    fun newDragFromTheTopReachesTheSheet() {
        assertFalse(sheetContentKeepsDrag(startedAtTop = true, contentMoved = false, availableY = 40f))
    }

    @Test
    fun flingThatRanUpTheListStopsAtItsTopEvenWhenItsStartLookedLikeTheTop() {
        assertTrue(sheetContentKeepsDrag(startedAtTop = true, contentMoved = true, availableY = 40f))
    }

    @Test
    fun upwardFlingRestNeverReachesTheOpenSheet() {
        assertTrue(sheetContentKeepsFling(startedAtTop = true, contentMoved = false, availableY = -900f))
        assertTrue(sheetContentKeepsFling(startedAtTop = false, contentMoved = true, availableY = -900f))
    }

    @Test
    fun downwardFlingRestFollowsTheDragRule() {
        assertTrue(sheetContentKeepsFling(startedAtTop = false, contentMoved = true, availableY = 900f))
        assertFalse(sheetContentKeepsFling(startedAtTop = true, contentMoved = false, availableY = 900f))
    }

    @Test
    fun upwardRemainderIsLeftToTheSheet() {
        assertFalse(sheetContentKeepsDrag(startedAtTop = false, contentMoved = true, availableY = -40f))
    }

    @Test
    fun scrollingBackToTheEndFollowsAgain() {
        assertTrue(followChatterMessagesEnd(following = false, previousMax = 360, value = 360, max = 360))
    }
}
