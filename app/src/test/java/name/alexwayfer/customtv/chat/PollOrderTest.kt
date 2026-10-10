package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class PollOrderTest {
    private fun poll(status: ChannelPollStatus, vararg votes: Pair<String, Int>) = ChannelPoll(
        id = "p1",
        title = "",
        status = status,
        choices = votes.map { (id, count) -> ChannelPollChoice(id = id, title = id, votes = count) },
        totalVotes = votes.sumOf { it.second },
        durationMillis = 0,
        endsAtMillis = 0,
    )

    @Test
    fun anOpenPollKeepsTheStreamersOrder() {
        val open = poll(ChannelPollStatus.Active, "a" to 1, "b" to 5, "c" to 3)
        assertEquals(listOf("a", "b", "c"), pollChoicesInOrder(open).map { it.id })
    }

    @Test
    fun anEndedPollGoesFromMostVotesToLeast() {
        val ended = poll(ChannelPollStatus.Ended, "a" to 1, "b" to 5, "c" to 3)
        assertEquals(listOf("b", "c", "a"), pollChoicesInOrder(ended).map { it.id })
    }

    @Test
    fun tiesKeepTheirOrder() {
        val ended = poll(ChannelPollStatus.Ended, "a" to 1, "b" to 2, "c" to 2)
        assertEquals(listOf("b", "c", "a"), pollChoicesInOrder(ended).map { it.id })
    }
}
