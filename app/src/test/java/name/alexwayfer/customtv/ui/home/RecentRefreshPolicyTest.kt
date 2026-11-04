package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentRefreshPolicyTest {
    @Test
    fun savedOrMissingRecentStatusDimsTheWholeList() {
        assertTrue(recentListDimmed(listOf(null, 1_000L), refreshing = false, nowMillis = 1_000L))
    }

    @Test
    fun recentListStaysDimmedUntilTheRefreshEnds() {
        assertTrue(recentListDimmed(listOf(1_000L, 1_000L), refreshing = true, nowMillis = 1_000L))
        assertFalse(recentListDimmed(listOf(1_000L, 1_000L), refreshing = false, nowMillis = 1_000L))
    }

    @Test
    fun anOldRecentStatusKeepsTheListDimmedAfterAFailedRefresh() {
        assertTrue(recentListDimmed(
            listOf(1_000L, 2_000L), refreshing = false, nowMillis = 1_000L + FOLLOWED_LIST_STALE_AFTER_MS,
        ))
    }

    @Test
    fun emptyRecentListHasNothingToDim() {
        assertFalse(recentListDimmed(emptyList(), refreshing = false, nowMillis = 1_000L))
    }

    @Test
    fun firstAutomaticRequestIsAllowed() {
        assertTrue(shouldAutomaticallyRefreshRecents(null, 0L))
    }

    @Test
    fun automaticRequestBeforeIntervalIsSkipped() {
        assertFalse(shouldAutomaticallyRefreshRecents(1_000L, 60_999L))
    }

    @Test
    fun automaticRequestAtIntervalIsAllowed() {
        assertTrue(shouldAutomaticallyRefreshRecents(1_000L, 61_000L))
    }

    @Test
    fun automaticRequestAfterClockResetIsAllowed() {
        assertTrue(shouldAutomaticallyRefreshRecents(5_000L, 1_000L))
    }

    @Test
    fun enteringMiniPlayerRefreshesWithoutProgress() {
        assertFalse(showAutomaticRecentRefreshProgress(firstHomeRefresh = false, returnedToForeground = false))
    }

    @Test
    fun openingOrResumingAppMayShowProgress() {
        assertTrue(showAutomaticRecentRefreshProgress(firstHomeRefresh = true, returnedToForeground = false))
        assertTrue(showAutomaticRecentRefreshProgress(firstHomeRefresh = false, returnedToForeground = true))
    }

    @Test
    fun signedOutRecentsRefreshInTheBackground() {
        assertTrue(refreshRecentsInBackground(signedIn = false, hasRecents = true))
    }

    @Test
    fun signedInRecentsDoNotRefreshInTheBackground() {
        assertFalse(refreshRecentsInBackground(signedIn = true, hasRecents = true))
    }

    @Test
    fun noRecentsDoNotRefreshInTheBackground() {
        assertFalse(refreshRecentsInBackground(signedIn = false, hasRecents = false))
    }

    @Test
    fun signedInRecentsRefreshWhenSuggestionsOpen() {
        assertTrue(refreshRecentsOnSearchOpen(signedIn = true, expanded = true, hasRecents = true))
    }

    @Test
    fun closingSuggestionsDoesNotRefreshRecents() {
        assertFalse(refreshRecentsOnSearchOpen(signedIn = true, expanded = false, hasRecents = true))
    }

    @Test
    fun signedOutSuggestionsLeaveTheRefreshToTheBackground() {
        assertFalse(refreshRecentsOnSearchOpen(signedIn = false, expanded = true, hasRecents = true))
    }

    @Test
    fun suggestionsWithoutRecentsDoNotRefresh() {
        assertFalse(refreshRecentsOnSearchOpen(signedIn = true, expanded = true, hasRecents = false))
    }
}
