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
    fun aChoiceOfAPollTheBuildCanVoteInIsPicked() {
        assertEquals(PollChoiceTap.Pick, pollChoiceTap(active = true, voting = voting(), premiumLocked = true))
    }

    @Test
    fun aChoiceOfAnOpenPollTheFreeBuildCannotVoteInOffersPremium() {
        assertEquals(PollChoiceTap.Premium, pollChoiceTap(active = true, voting = null, premiumLocked = true))
    }

    @Test
    fun aChoiceOfAnOpenPollWithoutVotingOrLockTakesNoTap() {
        assertEquals(PollChoiceTap.None, pollChoiceTap(active = true, voting = null, premiumLocked = false))
    }

    @Test
    fun aChoiceOfAnEndedPollTakesNoTapEvenLocked() {
        assertEquals(PollChoiceTap.None, pollChoiceTap(active = false, voting = null, premiumLocked = true))
    }

    @Test
    fun aChoiceOfAVotedPollTakesNoTap() {
        assertEquals(
            PollChoiceTap.None,
            pollChoiceTap(active = true, voting = voting(selected = "a", voted = "a"), premiumLocked = true),
        )
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
        val refused = voting(selected = "b", voted = "a", alreadyVoted = true)
        assertEquals(PollVoteStatus.AlreadyVoted, pollVoteStatus(refused))
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
