package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UniqueChatRowsTest {
    @Test
    fun aListWithoutRepeatsComesBackAsIs() {
        val messages = listOf(message("a", "one"), message("b", "two"))

        assertSame(messages, uniqueChatRows(messages))
    }

    @Test
    fun anEmptyListStaysEmpty() {
        assertEquals(emptyList<ChatMessage>(), uniqueChatRows(emptyList()))
    }

    @Test
    fun aMessageFromHistoryThatArrivesLiveKeepsOnlyItsFirstRow() {
        val fromHistory = message("a", "history")
        val later = message("b", "later")
        val live = message("a", "live")

        val result = uniqueChatRows(listOf(fromHistory, later, live))

        assertEquals(listOf(fromHistory, later), result)
    }

    @Test
    fun everyRepeatOfOneIdIsDropped() {
        val first = message("a", "one")

        val result = uniqueChatRows(listOf(first, message("a", "two"), message("a", "three")))

        assertEquals(listOf(first), result)
    }

    @Test
    fun theRowsAlwaysHaveDistinctKeys() {
        val ids = listOf("a", "b", "a", "c", "b", "c", "d")

        val result = uniqueChatRows(ids.mapIndexed { index, id -> message(id, "$index") })

        assertEquals(listOf("a", "b", "c", "d"), result.map { it.id })
    }

    private fun message(id: String, text: String) = ChatMessage(
        id = id,
        userLogin = "viewer",
        displayName = "Viewer",
        color = Color.White,
        rawText = text,
        parts = emptyList(),
        timestampMillis = 1_000,
    )
}
