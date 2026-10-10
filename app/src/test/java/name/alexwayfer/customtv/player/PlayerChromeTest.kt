package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerChromeTest {
    private val matched = mutableListOf<Boolean>()
    private val chrome = PlayerChrome().apply { matchControls = { matched += it } }

    @Test
    fun aSettledPlayerMatchesControlsAtOnce() {
        chrome.settle()
        chrome.request(true)
        assertEquals(listOf(true), matched)
    }

    @Test
    fun aRequestDuringTheAnimationWaitsForThePlayerToRest() {
        chrome.request(true)
        assertEquals(emptyList<Boolean>(), matched)
        chrome.settle()
        assertEquals(listOf(true), matched)
    }

    @Test
    fun theLatestWaitingRequestWins() {
        chrome.request(true)
        chrome.request(false)
        chrome.settle()
        assertEquals(listOf(false), matched)
    }

    @Test
    fun aWaitingRequestIsAppliedOnce() {
        chrome.request(true)
        chrome.settle()
        chrome.unsettle()
        chrome.settle()
        assertEquals(listOf(true), matched)
    }

    @Test
    fun anUnsettledPlayerHoldsNewRequests() {
        chrome.settle()
        chrome.unsettle()
        chrome.request(false)
        assertEquals(emptyList<Boolean>(), matched)
    }

    @Test
    fun hidingAsTheCollapseStartsDoesNotWaitAndDropsTheWaitingRequest() {
        chrome.request(true)
        chrome.hideNow()
        assertEquals(listOf(false), matched)
        chrome.settle()
        assertEquals(listOf(false), matched)
    }

    @Test
    fun settlingWithoutARequestDoesNotTouchControls() {
        chrome.settle()
        assertEquals(emptyList<Boolean>(), matched)
    }
}
