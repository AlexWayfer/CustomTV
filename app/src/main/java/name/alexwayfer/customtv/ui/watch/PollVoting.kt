package name.alexwayfer.customtv.ui.watch

/**
 * Voting in a poll from the chat card, where the build can vote. [votedChoiceId] is the vote Twitch took here;
 * [alreadyVoted] means Twitch refused the vote because the viewer had voted before; [failed] means the last vote was
 * not taken for another reason.
 */
internal data class PollVoting(
    val selectedChoiceId: String?,
    val votedChoiceId: String?,
    val sending: Boolean,
    val alreadyVoted: Boolean,
    val failed: Boolean,
    val onSelect: (String) -> Unit,
    val onVote: () -> Unit,
)

/** Choices take a pick while the poll is open and the viewer has not voted yet. */
internal fun pollChoicesPickable(active: Boolean, voting: PollVoting?): Boolean =
    active && voting != null && voting.votedChoiceId == null

internal enum class PollChoiceTap {
    Pick,
    Premium,
    None,
}

/**
 * What a tap on a choice does: picks it where the build can vote, or, on an open poll the free build cannot vote in
 * ([premiumLocked]), shows the Premium banner. A poll that ended takes no tap.
 */
internal fun pollChoiceTap(active: Boolean, voting: PollVoting?, premiumLocked: Boolean): PollChoiceTap = when {
    pollChoicesPickable(active, voting) -> PollChoiceTap.Pick
    active && voting == null && premiumLocked -> PollChoiceTap.Premium
    else -> PollChoiceTap.None
}

/**
 * Vote, shown only on an open poll, sends the picked choice once; it stays off while the vote is on its way
 * and after Twitch took it.
 */
internal fun pollVoteEnabled(voting: PollVoting): Boolean =
    voting.votedChoiceId == null && voting.selectedChoiceId != null && !voting.sending

internal enum class PollVoteStatus {
    None,
    Voted,
    AlreadyVoted,
    Failed,
}

/**
 * What the card says beside Vote: "Already voted!" when Twitch refused a vote the viewer had already cast, here or on
 * another device, "Voted!" for any other known vote, and the failure only while no vote is known.
 */
internal fun pollVoteStatus(voting: PollVoting): PollVoteStatus = when {
    voting.votedChoiceId != null && voting.alreadyVoted -> PollVoteStatus.AlreadyVoted
    voting.votedChoiceId != null -> PollVoteStatus.Voted
    voting.failed -> PollVoteStatus.Failed
    else -> PollVoteStatus.None
}
