package name.alexwayfer.customtv.data

import name.alexwayfer.customtv.chat.TwitchCatalogEmote
import name.alexwayfer.customtv.chat.TwitchEmoteGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserEmotePagesTest {
    private val cursors = mutableListOf<String?>()

    private fun page(name: String, next: String?, type: String = "smilies") = UserEmotePageAnswer.Ready(
        UserTwitchEmotePage(
            smilies = listOf(TwitchCatalogEmote(name, "https://emote/$name", TwitchEmoteGroup.Global)),
            turbo = emptyList(),
            cursor = next,
            typeCounts = mapOf(type to 1),
        ),
        body = "body $name",
    )

    /** Reads the [answers] in order, one per page asked for. */
    private fun read(vararg answers: UserEmotePageAnswer, maxPages: Int = 40): UserEmotePages {
        val queue = ArrayDeque(answers.toList())
        return readUserEmotePages(maxPages) { cursor ->
            cursors += cursor
            queue.removeFirst()
        }
    }

    private fun UserEmotePages.names(): List<String> =
        (this as UserEmotePages.Read).emotes.smilies.map { it.name }

    @Test
    fun thePagesAreReadInOrderByTheirCursorsToTheLast() {
        val result = read(page("A", "c1"), page("B", "c2", type = "follower"), page("C", null))

        assertEquals(listOf(null, "c1", "c2"), cursors)
        assertEquals(listOf("A", "B", "C"), result.names())
        result as UserEmotePages.Read
        assertEquals(3, result.pages)
        assertTrue(result.complete)
        assertEquals(listOf("body A", "body B", "body C"), result.bodies)
        assertEquals(mapOf("smilies" to 2, "follower" to 1), result.typeCounts)
    }

    @Test
    fun aCursorAlreadyReadEndsThePages() {
        val result = read(page("A", "c1"), page("B", "c1"))

        assertEquals(listOf("A", "B"), result.names())
        assertTrue((result as UserEmotePages.Read).complete)
    }

    @Test
    fun theLimitEndsThePagesUnfinished() {
        val result = read(page("A", "c1"), page("B", "c2"), maxPages = 2)

        assertEquals(listOf("A", "B"), result.names())
        result as UserEmotePages.Read
        assertEquals(2, result.pages)
        assertEquals(false, result.complete)
    }

    @Test
    fun aPageThatDoesNotAnswerGivesWhatCameBefore() {
        val result = read(page("A", "c1"), UserEmotePageAnswer.Unavailable)

        assertEquals(
            listOf("A"),
            (result as UserEmotePages.Unavailable).partial.smilies.map { it.name },
        )
    }

    @Test
    fun aRefusalSaysWhetherItCameOnTheFirstPage() {
        assertEquals(UserEmotePages.Rejected(firstPage = true), read(UserEmotePageAnswer.Rejected))
        assertEquals(
            UserEmotePages.Rejected(firstPage = false),
            read(page("A", "c1"), UserEmotePageAnswer.Rejected),
        )
    }
}
