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
    fun aSentMessageCompletesTheSend() {
        assertTrue(chatSendCompleted(ChatSendResult.Sent, draftMatchesSentMessage = true))
        assertFalse(chatSendCompleted(ChatSendResult.Rejected(401, ""), draftMatchesSentMessage = true))
        assertFalse(chatSendCompleted(ChatSendResult.Sent, draftMatchesSentMessage = false))
    }

    @Test
    fun aStoppedChatJumpsToTheLatestAfterSendWhenTheSettingIsOn() {
        assertTrue(chatFollowsAfterSend(following = false, scrollToLatestAfterSend = true))
    }

    @Test
    fun aStoppedChatStaysInPlaceAfterSendWhenTheSettingIsOff() {
        assertFalse(chatFollowsAfterSend(following = false, scrollToLatestAfterSend = false))
    }

    @Test
    fun aFollowingChatKeepsFollowingAfterSendEitherWay() {
        assertTrue(chatFollowsAfterSend(following = true, scrollToLatestAfterSend = false))
        assertTrue(chatFollowsAfterSend(following = true, scrollToLatestAfterSend = true))
    }
}
