package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatUserCommandTest {
    @Test
    fun aNickWithAtAndUpperCaseOpensItsLowerCaseLogin() {
        assertEquals(ChatUserCommand.Open("some_user"), chatUserCommand("/user @Some_User", emptyMap()))
    }

    @Test
    fun aNickWithoutAtOpensItsLogin() {
        assertEquals(ChatUserCommand.Open("someuser"), chatUserCommand("  /USER SomeUser  ", emptyMap()))
    }

    @Test
    fun aCompletedDisplayNameOpensTheChattersLogin() {
        val chatters = mapOf("koreanlogin" to "한국어")
        assertEquals(ChatUserCommand.Open("koreanlogin"), chatUserCommand("/user @한국어 ", chatters))
    }

    @Test
    fun wordsAfterTheNickAreIgnored() {
        assertEquals(ChatUserCommand.Open("someuser"), chatUserCommand("/user someuser hello", emptyMap()))
    }

    @Test
    fun theCommandWithoutANickIsInvalid() {
        assertEquals(ChatUserCommand.Invalid, chatUserCommand("/user", emptyMap()))
        assertEquals(ChatUserCommand.Invalid, chatUserCommand("/user @", emptyMap()))
    }

    @Test
    fun aNickThatCannotBeALoginIsInvalid() {
        assertEquals(ChatUserCommand.Invalid, chatUserCommand("/user some-user", emptyMap()))
    }

    @Test
    fun otherTextIsAMessage() {
        assertNull(chatUserCommand("hello /user someuser", emptyMap()))
        assertNull(chatUserCommand("/username", emptyMap()))
    }

    @Test
    fun aChatterWhoWroteHereGetsTheNameIdAndBadgesOfTheLatestMessage() {
        val badge = ChatBadge("subscriber", "12")
        val request = chatterCardRequestForLogin(
            "someuser",
            listOf(message("SomeUser", "Old", emptyList()), message("someuser", "SomeUser", listOf(badge))),
            mapOf("subscriber/12" to "url"),
        )
        assertEquals("SomeUser", request.displayName)
        assertEquals("id-someuser", request.userId)
        assertEquals(listOf(badge), request.badges)
        assertEquals(mapOf("subscriber/12" to "url"), request.badgeUrls)
    }

    @Test
    fun aChatterWithoutMessagesHereOpensByLoginAlone() {
        val request = chatterCardRequestForLogin("someuser", listOf(message("other", "Other", emptyList())), emptyMap())
        assertEquals("someuser", request.displayName)
        assertNull(request.userId)
        assertEquals(emptyList<ChatBadge>(), request.badges)
    }

    private fun message(login: String, displayName: String, badges: List<ChatBadge>) = ChatMessage(
        id = "$login-$displayName",
        userLogin = login,
        displayName = displayName,
        color = Color.White,
        rawText = "hi",
        parts = emptyList(),
        timestampMillis = 0,
        badges = badges,
        userId = "id-${login.lowercase()}",
    )
}
