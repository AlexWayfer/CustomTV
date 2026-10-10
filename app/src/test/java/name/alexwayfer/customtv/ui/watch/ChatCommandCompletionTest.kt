package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.NickCompletionEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCommandCompletionTest {
    @Test
    fun aSlashAloneOffersEveryCommand() {
        val query = chatCommandQueryAtCursor("/", cursor = 1)
        assertEquals(ChatCommandQuery(end = 1, text = "/"), query)
        assertEquals(ChatCommand.entries.toList(), matchingChatCommands(query!!.text))
    }

    @Test
    fun aTypedPrefixInAnyCaseOffersTheCommand() {
        assertEquals(listOf(ChatCommand.User), matchingChatCommands("/US"))
    }

    @Test
    fun anUnknownCommandOffersNothing() {
        assertEquals(emptyList<ChatCommand>(), matchingChatCommands("/ban"))
    }

    @Test
    fun aSlashOutsideTheStartOrAfterTheCommandWordIsNoQuery() {
        assertNull(chatCommandQueryAtCursor("hi /us", cursor = 6))
        assertNull(chatCommandQueryAtCursor("/user some", cursor = 10))
        assertNull(chatCommandQueryAtCursor(" /us", cursor = 4))
    }

    @Test
    fun theCursorInsideTheWordTakesTheWholeWord() {
        assertEquals(ChatCommandQuery(end = 4, text = "/use"), chatCommandQueryAtCursor("/use nick", cursor = 2))
    }

    @Test
    fun pickingACommandAddsASpaceAndPutsTheCursorAfterIt() {
        assertEquals(NickCompletionEdit("/user ", 6), textWithCompletedCommand("/us", 3, ChatCommand.User, maxLength = 500))
    }

    @Test
    fun pickingACommandKeepsTheTextAfterItWithOneSpace() {
        assertEquals(
            NickCompletionEdit("/user nick", 6),
            textWithCompletedCommand("/us nick", 3, ChatCommand.User, maxLength = 500),
        )
    }

    @Test
    fun aCommandThatDoesNotFitIsNotInserted() {
        assertNull(textWithCompletedCommand("/u", 2, ChatCommand.User, maxLength = 5))
    }
}
