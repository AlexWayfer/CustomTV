package name.alexwayfer.customtv.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramChecksTest {
    @Test
    fun phoneCodeAndPasswordStaySeparateFromReady() {
        assertEquals(TelegramLoginStep.Phone, telegramLoginStep(TelegramAuthSignal.WaitPhoneNumber))
        assertEquals(TelegramLoginStep.Confirm, telegramLoginStep(TelegramAuthSignal.WaitOtherDevice))
        assertEquals(TelegramLoginStep.Code, telegramLoginStep(TelegramAuthSignal.WaitCode))
        assertEquals(TelegramLoginStep.Password, telegramLoginStep(TelegramAuthSignal.WaitPassword))
        assertEquals(TelegramLoginStep.Ready, telegramLoginStep(TelegramAuthSignal.Ready))
        assertEquals(TelegramLoginStep.LoggingOut, telegramLoginStep(TelegramAuthSignal.LoggingOut))
        assertEquals(TelegramLoginStep.Starting, telegramLoginStep(TelegramAuthSignal.Closed))
        assertEquals(TelegramLoginStep.Unsupported, telegramLoginStep(TelegramAuthSignal.WaitRegistration))
    }

    @Test
    fun editionGroupLinkMatchesTheBuild() {
        assertEquals(
            "https://t.me/c/1234567890/3",
            telegramEditionGroupLink(
                premium = false,
                openChatId = -1001234567890L,
                openTopicId = 3,
                premiumChatId = -1002345678901L,
                premiumTopicId = 9,
            ),
        )
        assertEquals(
            "https://t.me/c/2345678901/9",
            telegramEditionGroupLink(
                premium = true,
                openChatId = -1001234567890L,
                openTopicId = 3,
                premiumChatId = -1002345678901L,
                premiumTopicId = 9,
            ),
        )
    }

    @Test
    fun forumReadsOnlyTheConfiguredTopic() {
        assertEquals(TelegramHistoryRequest.Topic(65), telegramHistoryRequest(chatId = -1001L, forum = true, topicId = 65))
        assertEquals(TelegramHistoryRequest.TopicMissing, telegramHistoryRequest(chatId = -1001L, forum = true, topicId = 0))
        assertEquals(TelegramHistoryRequest.WholeChat, telegramHistoryRequest(chatId = -1001L, forum = false, topicId = 65))
        assertEquals(TelegramHistoryRequest.ChatMissing, telegramHistoryRequest(chatId = 0L, forum = true, topicId = 65))
    }

    @Test
    fun supergroupLinkUsesTheChannelIdAndTopic() {
        assertEquals(
            "https://t.me/c/1234567890/3",
            telegramSupergroupLink(chatId = -1001234567890L, topicId = 3),
        )
        assertNull(telegramSupergroupLink(chatId = -1234567890L, topicId = 3))
        assertNull(telegramSupergroupLink(chatId = -1001234567890L, topicId = 0))
    }

    @Test
    fun missingChatIsUnavailableAndATimeoutIsNot() {
        assertTrue(telegramChatUnavailable(400, "Chat not found"))
        assertTrue(telegramChatUnavailable(400, "CHAT_NOT_ACCESSIBLE"))
        assertFalse(telegramChatUnavailable(500, "Chat not found"))
        assertFalse(telegramChatUnavailable(400, "Timeout"))
        assertFalse(telegramChatUnavailable(400, "PHONE_NUMBER_INVALID"))
    }

    @Test
    fun accountLabelJoinsNameAndUsername() {
        assertEquals("Alex Way @alex", telegramAccountLabel("Alex", "Way", "alex"))
        assertEquals("@alex", telegramAccountLabel(" ", "", "alex"))
        assertEquals("", telegramAccountLabel("", "", null))
    }
}
