package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.chat.NickCompletionEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCommandCompletionTest {
    @Test
    fun aSlashAloneOffersEveryCommandTheUserHas() {
        val query = chatCommandQueryAtCursor("/", cursor = 1)
        assertEquals(ChatCommandQuery(end = 1, text = "/"), query)
        assertEquals(
            ChatCommand.entries.toList(),
            matchingChatCommands(query!!.text, availableChatCommands(predictionOpenable = true)),
        )
    }

    @Test
    fun aTypedPrefixInAnyCaseOffersTheCommand() {
        assertEquals(listOf(ChatCommand.User), matchingChatCommands("/US", ChatCommand.entries))
    }

    @Test
    fun anUnknownCommandOffersNothing() {
        assertEquals(emptyList<ChatCommand>(), matchingChatCommands("/ban", ChatCommand.entries))
    }

    @Test
    fun theStreamerAndModeratorsGetPrediction() {
        assertEquals(
            listOf(ChatCommand.Prediction),
            matchingChatCommands("/pre", availableChatCommands(predictionOpenable = true)),
        )
    }

    @Test
    fun aViewerHasNoPredictionCommand() {
        assertEquals(listOf(ChatCommand.User), availableChatCommands(predictionOpenable = false))
    }

    @Test
    fun predictionAloneInAnyCaseIsTheCommand() {
        assertTrue(isPredictionCommand("/prediction"))
        assertTrue(isPredictionCommand(" /Prediction "))
    }

    @Test
    fun predictionWithMoreTextIsAMessage() {
        assertFalse(isPredictionCommand("/prediction now"))
        assertFalse(isPredictionCommand("what /prediction"))
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
        assertEquals(
            NickCompletionEdit("/user ", 6),
            textWithCompletedCommand("/us", 3, ChatCommand.User, maxLength = 500),
        )
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
