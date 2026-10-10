package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.ChatSendResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSendFeedbackTest {
    @Test
    fun sendHidesTheKeyboardByDefault() {
        assertTrue(chatSendHidesKeyboard(keepKeyboardAfterSend = false))
    }

    @Test
    fun sendKeepsTheKeyboardWhenTheSettingIsOn() {
        assertFalse(chatSendHidesKeyboard(keepKeyboardAfterSend = true))
    }

    @Test
    fun aSentMessageReturnsTheChatToTheLatestAndClosesAnOpenThread() {
        assertTrue(chatSendReturnsToLatest(ChatSendResult.Sent, draftMatchesSentMessage = true))
        assertFalse(chatSendReturnsToLatest(ChatSendResult.Rejected(401, ""), draftMatchesSentMessage = true))
        assertFalse(chatSendReturnsToLatest(ChatSendResult.Sent, draftMatchesSentMessage = false))
    }
}
