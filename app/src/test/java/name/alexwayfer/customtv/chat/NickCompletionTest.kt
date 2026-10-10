package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NickCompletionTest {
    @Test
    fun anAtSignAloneDoesNotAskForNicks() {
        assertNull(nickQueryAtCursor("@", cursor = 1))
        assertNull(nickQueryAtCursor("hi @", cursor = 4))
    }

    @Test
    fun aNickCharacterAfterAtAsksForMatchingNicks() {
        assertEquals("a", nickQueryAtCursor("@a", cursor = 2)?.text)
        assertEquals("bo", nickQueryAtCursor("hi @bo there", cursor = 6)?.text)
        assertEquals("_x", nickQueryAtCursor("@_x", cursor = 3)?.text)
    }

    @Test
    fun anAtSignInsideAWordIsNotAMention() {
        assertNull(nickQueryAtCursor("mail@host", cursor = 9))
    }

    @Test
    fun aRepeatedMessageKeepsTheDisplayNameAndDoesNotReorder() {
        val first = rememberSessionChatter(emptyMap(), chatter("alex", "Alex"))
        val again = rememberSessionChatter(first.chatters, chatter("sam", "Sam"))
        val renamed = rememberSessionChatter(again.chatters, chatter("alex", "AlexW"))
        val samAgain = rememberSessionChatter(renamed.chatters, chatter("sam", "Sammy"))
        assertEquals(mapOf("alex" to "AlexW", "sam" to "Sammy"), samAgain.chatters)
        assertFalse(samAgain.firstInSession)
        assertEquals(samAgain.chatters, rememberSessionChatter(samAgain.chatters, chatter("sam", "Sammy")).chatters)
        val notice = rememberSessionChatter(samAgain.chatters, chatter("bot", "Bot").copy(notice = ChatNotice.Welcome))
        assertEquals(samAgain.chatters, notice.chatters)
    }

    @Test
    fun suggestionsMatchTheDisplayNameOrLoginPrefix() {
        val chatters = mapOf(
            "new" to "Newbie",
            "alexwayfer" to "Alex",
            "nora" to "Someone",
        )
        assertEquals(
            listOf("new", "nora"),
            matchingSessionNicks(chatters, "n").map { it.login },
        )
        assertEquals(listOf("alexwayfer"), matchingSessionNicks(chatters, "ale").map { it.login })
        assertEquals(8, matchingSessionNicks((0 until 8).associate { "a$it" to "A$it" }, "a").size)
    }

    @Test
    fun suggestionsAreAlphabeticalAndTheChannelOwnerIsFirst() {
        val chatters = mapOf(
            "azoe" to "Zoe",
            "aamy" to "Amy",
            "ahost" to "Host",
        )
        val owner = ChatNick("ahost", "Host")
        assertEquals(
            listOf("aamy", "ahost", "azoe"),
            matchingSessionNicks(chatters, "a").map { it.login },
        )
        assertEquals(
            listOf("ahost", "aamy", "azoe"),
            matchingSessionNicks(chatters, "a", owner).map { it.login },
        )
    }

    @Test
    fun aMatchingChannelOwnerIsTheFirstSuggestion() {
        val chatters = mapOf(
            "helen" to "Helen",
            "host" to "HostName",
        )
        val owner = ChatNick("host", "HostName")
        assertEquals(
            listOf("host", "helen"),
            matchingSessionNicks(chatters, "h", owner).map { it.login },
        )
        assertEquals(
            listOf("host"),
            matchingSessionNicks(emptyMap(), "host", owner).map { it.login },
        )
        assertEquals(
            listOf("helen"),
            matchingSessionNicks(chatters, "hel", owner).map { it.login },
        )
    }

    @Test
    fun choosingANickReplacesTheQueryAndLeavesOneSpace() {
        val completed = textWithCompletedNick("hi @al", cursor = 6, displayName = "Alex", maxLength = 500)
        assertEquals("hi @Alex ", completed?.text)
        assertEquals(9, completed?.cursor)
        val spaced = textWithCompletedNick("hi @al there", cursor = 6, displayName = "Alex", maxLength = 500)
        assertEquals("hi @Alex there", spaced?.text)
        assertEquals(9, spaced?.cursor)
        assertNull(textWithCompletedNick("@alex", cursor = 5, displayName = "Alexander", maxLength = 8))
    }

    private fun chatter(login: String, displayName: String) = ChatMessage(
        id = login,
        userLogin = login,
        displayName = displayName,
        color = Color.Unspecified,
        rawText = "hi",
        parts = emptyList(),
        timestampMillis = 1L,
    )
}
