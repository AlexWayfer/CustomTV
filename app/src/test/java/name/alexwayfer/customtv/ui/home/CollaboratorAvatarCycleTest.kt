package name.alexwayfer.customtv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class CollaboratorAvatarCycleTest {
    @Test
    fun oneAvatarNeverChanges() {
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 0, startedAtMillis = 0, count = 1))
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 123_456, startedAtMillis = 0, count = 1))
    }

    @Test
    fun startedOnABoundaryTheAvatarsAdvanceEveryTwoAndAHalfSecondsAndWrapAround() {
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 2_499, startedAtMillis = 0, count = 3))
        assertEquals(1, collaboratorAvatarIndex(uptimeMillis = 2_500, startedAtMillis = 0, count = 3))
        assertEquals(2, collaboratorAvatarIndex(uptimeMillis = 5_000, startedAtMillis = 0, count = 3))
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 7_500, startedAtMillis = 0, count = 3))
    }

    @Test
    fun startedJustBeforeABoundaryTheFirstAvatarStillGetsAWholeInterval() {
        val startedAt = 2_400L
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 2_500, startedAtMillis = startedAt, count = 3))
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 4_999, startedAtMillis = startedAt, count = 3))
        assertEquals(1, collaboratorAvatarIndex(uptimeMillis = 5_000, startedAtMillis = startedAt, count = 3))
        assertEquals(2, collaboratorAvatarIndex(uptimeMillis = 7_500, startedAtMillis = startedAt, count = 3))
    }

    @Test
    fun theFirstSwitchWaitsForTheBoundaryAfterAWholeIntervalThenFollowsTheSharedBoundaries() {
        assertEquals(2_600L, millisUntilNextCollaboratorAvatar(uptimeMillis = 2_400, startedAtMillis = 2_400))
        assertEquals(2_500L, millisUntilNextCollaboratorAvatar(uptimeMillis = 2_500, startedAtMillis = 0))
        assertEquals(2_500L, millisUntilNextCollaboratorAvatar(uptimeMillis = 5_000, startedAtMillis = 2_400))
        assertEquals(1L, millisUntilNextCollaboratorAvatar(uptimeMillis = 7_499, startedAtMillis = 2_400))
    }

    @Test
    fun aRowBackInViewWithTheRememberedStartContinuesTheCircleWithoutAnExtraHold() {
        val startedAt = 2_400L
        assertEquals(0, collaboratorAvatarIndex(uptimeMillis = 12_400, startedAtMillis = startedAt, count = 3))
        assertEquals(100L, millisUntilNextCollaboratorAvatar(uptimeMillis = 12_400, startedAtMillis = startedAt))
    }
}
