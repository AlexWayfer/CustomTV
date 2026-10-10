package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PollVotingTest {
    private fun voting(
        selected: String? = null,
        voted: String? = null,
        sending: Boolean = false,
        alreadyVoted: Boolean = false,
        failed: Boolean = false,
    ) = PollVoting(
        selectedChoiceId = selected,
        votedChoiceId = voted,
        sending = sending,
        alreadyVoted = alreadyVoted,
        failed = failed,
        onSelect = {},
        onVote = {},
    )

    @Test
    fun anOpenPollNotVotedYetTakesAPick() {
        assertTrue(pollChoicesPickable(active = true, voting = voting()))
    }

    @Test
    fun aBuildThatCannotVoteTakesNoPick() {
        assertFalse(pollChoicesPickable(active = true, voting = null))
    }

    @Test
    fun anEndedPollTakesNoPick() {
        assertFalse(pollChoicesPickable(active = false, voting = voting(selected = "a")))
    }

    @Test
    fun aVotedPollTakesNoMorePicks() {
        assertFalse(pollChoicesPickable(active = true, voting = voting(selected = "a", voted = "a")))
    }

    @Test
    fun voteIsOffUntilAChoiceIsPicked() {
        assertFalse(pollVoteEnabled(voting = voting()))
        assertTrue(pollVoteEnabled(voting = voting(selected = "a")))
    }

    @Test
    fun voteIsOffWhileTheVoteIsOnItsWay() {
        assertFalse(pollVoteEnabled(voting = voting(selected = "a", sending = true)))
    }

    @Test
    fun voteIsOffOnceTwitchTookTheVote() {
        assertFalse(pollVoteEnabled(voting = voting(selected = "a", voted = "a")))
    }

    @Test
    fun aVoteTwitchTookSaysVoted() {
        assertEquals(PollVoteStatus.Voted, pollVoteStatus(voting(selected = "a", voted = "a")))
    }

    @Test
    fun aRefusedVoteOverAnEarlierOneSaysAlreadyVoted() {
        assertEquals(PollVoteStatus.AlreadyVoted, pollVoteStatus(voting(selected = "b", voted = "a", alreadyVoted = true)))
    }

    @Test
    fun aRefusedVoteWithNoVoteKnownSaysItFailed() {
        assertEquals(PollVoteStatus.Failed, pollVoteStatus(voting(selected = "a", failed = true)))
    }

    @Test
    fun nothingIsSaidBeforeAVote() {
        assertEquals(PollVoteStatus.None, pollVoteStatus(voting(selected = "a")))
    }
}
