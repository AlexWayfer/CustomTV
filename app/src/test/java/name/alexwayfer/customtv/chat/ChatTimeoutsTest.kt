package name.alexwayfer.customtv.chat

import name.alexwayfer.customtv.data.ChatSendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTimeoutsTest {
    private val timeouts = ChatTimeouts().apply { setUser("456") }

    @Test
    fun timeoutUsesTheServerTimestampAndOnlyBlocksItsChannelAndAccount() {
        timeouts.onMessage("dallas", event("456", 1_000, 60))

        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))
        assertNull(timeouts.state.value.deadline("another", "456"))
        assertNull(timeouts.state.value.deadline("dallas", "123"))
        timeouts.setUser("123")
        assertNull(timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun anotherChattersTimeoutCannotBlockTheField() {
        timeouts.onMessage("dallas", event("123", 1_000, 60))

        assertNull(timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun aNewerTimeoutCanShortenTheWaitAndOlderHistoryCannotRestoreIt() {
        val older = event("456", 1_000, 60)
        timeouts.onMessage("dallas", older)
        timeouts.onMessage("dallas", event("456", 2_000, 5))
        timeouts.onHistory("dallas", RecentChatLoadResult(moderation = listOf(older)))

        assertEquals(7_000L, timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun historyCanRestoreAnActiveTimeoutButALaterOwnMessageClearsIt() {
        val older = event("456", 1_000, 60)
        timeouts.onHistory("dallas", RecentChatLoadResult(moderation = listOf(older)))
        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))

        val ownMessage = older.copy(
            eventKind = ChatEventKind.Normal,
            timeoutSeconds = null,
            timestampMillis = 2_000,
        )
        timeouts.onMessage("dallas", ownMessage.copy(sourceRoomId = "another"))
        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))

        timeouts.onMessage("dallas", ownMessage)
        timeouts.onHistory("dallas", RecentChatLoadResult(moderation = listOf(older)))
        assertNull(timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun aHistoryResponseRecoversTheTimeoutWithoutAddingAModerationRow() {
        val history = parseRecentChat(
            """
            {"messages":[
                "@user-id=789;tmi-sent-ts=900 :another!another@tmi.twitch.tv PRIVMSG #dallas :hello",
                "@ban-duration=60;target-user-id=456;tmi-sent-ts=1000 :tmi.twitch.tv CLEARCHAT #dallas :viewer"
            ]}
            """.trimIndent(),
            count = 1,
        )
        timeouts.onHistory("dallas", history)

        assertEquals("hello", history.messages.single().rawText)
        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun showsTheLastPartialSecondAndUnlocksAtTheDeadline() {
        assertEquals(2L, chatTimeoutSecondsLeft(3_000L, 1_999L))
        assertEquals(1L, chatTimeoutSecondsLeft(3_000L, 2_999L))
        assertEquals(0L, chatTimeoutSecondsLeft(3_000L, 3_000L))
        assertEquals(0L, chatTimeoutSecondsLeft(3_000L, 4_000L))
        assertEquals(0L, chatTimeoutSecondsLeft(null, 2_000L))
    }

    @Test
    fun helixTimeoutIsRecognizedByCodeWithoutParsingItsMessage() {
        assertTrue(chatSendTimedOut(ChatSendResult.Dropped("user_timed_out", "")))
        assertFalse(chatSendTimedOut(ChatSendResult.Dropped("user_banned", "Timeout")))
        assertFalse(chatSendTimedOut(ChatSendResult.Sent))
    }

    @Test
    fun aStatusCheckReadBeforeANewerIrcEventCannotClearThatTimeout() {
        val beforeCheck = timeouts.state.value
        timeouts.onMessage("dallas", event("456", 1_000, 60))

        assertFalse(timeouts.applyCheck("dallas", "456", beforeCheck, ChatTimeoutCheck.Clear, 500))
        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun historyFromBeforeAStatusCheckCannotHideItsNewerDeadline() {
        val beforeCheck = timeouts.state.value
        timeouts.onHistory("dallas", RecentChatLoadResult(moderation = listOf(event("456", 1_000, 60))))

        assertTrue(timeouts.applyCheck("dallas", "456", beforeCheck, ChatTimeoutCheck.Until(91_000), 2_000))
        assertEquals(91_000L, timeouts.state.value.deadline("dallas", "456"))
    }

    @Test
    fun aStatusCheckCanRecoverTheDeadlineAndClearItAfterAnEarlyRelease() {
        assertTrue(timeouts.applyCheck("dallas", "456", timeouts.state.value, ChatTimeoutCheck.Until(61_000), 2_000))
        assertEquals(61_000L, timeouts.state.value.deadline("dallas", "456"))

        assertTrue(timeouts.applyCheck("dallas", "456", timeouts.state.value, ChatTimeoutCheck.Clear, 3_000))
        assertNull(timeouts.state.value.deadline("dallas", "456"))
    }

    private fun event(userId: String, at: Long, seconds: Long): ChatMessage =
        requireNotNull(
            IrcMessageParser.parseClearChat(
                "@ban-duration=$seconds;target-user-id=$userId;tmi-sent-ts=$at " +
                    ":tmi.twitch.tv CLEARCHAT #dallas :viewer",
            ),
        )
}
