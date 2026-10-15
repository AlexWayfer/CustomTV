package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionInsertTest {
    @Test
    fun anEmptyFieldGetsTheMentionAndASpaceWithTheCursorAfterIt() {
        assertEquals(EmoteInsertion("@Nick ", 6), textWithInsertedMention("", 0, 0, "Nick", maxLength = 500))
    }

    @Test
    fun typedTextGetsASpaceBeforeTheMention() {
        assertEquals(
            EmoteInsertion("hello @Nick ", 12),
            textWithInsertedMention("hello", 5, 5, "Nick", maxLength = 500),
        )
    }

    @Test
    fun typedTextEndingInASpaceGetsNoSecondOne() {
        assertEquals(
            EmoteInsertion("hello @Nick ", 12),
            textWithInsertedMention("hello ", 6, 6, "Nick", maxLength = 500),
        )
    }

    @Test
    fun theMentionGoesWhereTheCursorIs() {
        assertEquals(
            EmoteInsertion("@Nick hello", 6),
            textWithInsertedMention("hello", 0, 0, "Nick", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion("hi @Nick there", 9),
            textWithInsertedMention("hi there", 3, 3, "Nick", maxLength = 500),
        )
    }

    @Test
    fun aSelectionIsReplacedByTheMention() {
        assertEquals(
            EmoteInsertion("hi @Nick ", 9),
            textWithInsertedMention("hi there", 3, 8, "Nick", maxLength = 500),
        )
    }

    @Test
    fun aMentionPastTheLimitIsNotInserted() {
        assertNull(textWithInsertedMention("hello", 5, 5, "Nick", maxLength = 10))
    }

    @Test
    fun aBlankNameInsertsNothing() {
        assertNull(textWithInsertedMention("hello", 5, 5, " ", maxLength = 500))
    }

    @Test
    fun aChattersMessageCanBeMentionedAndANoticeCannot() {
        assertTrue(chatMessageCanBeMentioned(testChatMessage(login = "viewer")))
        assertFalse(chatMessageCanBeMentioned(testChatMessage(login = "")))
    }

    @Test
    fun aDisplayNameThatSpellsTheLoginIsMentioned() {
        assertEquals("NickName", chatMentionName("nickname", "NickName"))
    }

    @Test
    fun aLocalizedDisplayNameIsMentionedByTheLogin() {
        assertEquals("nickname", chatMentionName("nickname", "Никнейм"))
        assertEquals("nickname", chatMentionName("nickname", ""))
    }
}
