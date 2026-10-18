package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectGapTest {
    private fun notice(notice: ChatNotice, id: String) = chatNoticeMessage(notice, id)

    @Test
    fun connectingBeforeTheWelcomeIsNotAReconnect() {
        assertFalse(chatReconnecting(welcomed = false, ChatConnectionState.Connecting))
        assertFalse(chatReconnecting(welcomed = false, ChatConnectionState.Reconnecting))
    }

    @Test
    fun lostConnectionAfterTheWelcomeReconnectsUntilConnected() {
        assertTrue(chatReconnecting(welcomed = true, ChatConnectionState.Reconnecting))
        assertTrue(chatReconnecting(welcomed = true, ChatConnectionState.Connecting))
        assertFalse(chatReconnecting(welcomed = true, ChatConnectionState.Connected))
    }

    @Test
    fun closedConnectionIsNotAReconnect() {
        assertFalse(chatReconnecting(welcomed = true, ChatConnectionState.Disconnected))
    }

    @Test
    fun disconnectBeforeConnectedIsTheGap() {
        val messages = listOf(
            notice(ChatNotice.Welcome, "welcome"),
            testChatMessage(id = "a"),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
        )

        assertEquals("lost", reconnectGapNoticeId(messages))
    }

    @Test
    fun failedAttemptsBetweenDisconnectAndConnectedKeepTheGap() {
        val messages = listOf(
            notice(ChatNotice.Welcome, "welcome"),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Reconnecting, "try-1"),
            testChatMessage(id = "pubsub"),
            notice(ChatNotice.Connected, "back"),
        )

        assertEquals("lost", reconnectGapNoticeId(messages))
    }

    @Test
    fun connectedWithoutAShownDisconnectHasNoGap() {
        val messages = listOf(
            notice(ChatNotice.Disconnected, "old"),
            notice(ChatNotice.Connected, "old-back"),
            testChatMessage(id = "a"),
            notice(ChatNotice.Connected, "back"),
        )

        assertNull(reconnectGapNoticeId(messages))
    }

    @Test
    fun chatStillDownHasNoGap() {
        val messages = listOf(
            notice(ChatNotice.Welcome, "welcome"),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Reconnecting, "try-1"),
        )

        assertNull(reconnectGapNoticeId(messages))
    }

    @Test
    fun missedMessagesGoAfterTheDisconnectOldestFirst() {
        val messages = listOf(
            notice(ChatNotice.Welcome, "welcome"),
            testChatMessage(id = "a", timestampMillis = 100L),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
            testChatMessage(id = "d", timestampMillis = 400L),
        )
        val recent = listOf(
            testChatMessage(id = "old", timestampMillis = 50L),
            testChatMessage(id = "a", timestampMillis = 100L),
            testChatMessage(id = "c", timestampMillis = 300L),
            testChatMessage(id = "b", timestampMillis = 200L),
            testChatMessage(id = "d", timestampMillis = 400L),
        )

        val merge = mergeReconnectGap(messages, recent, "lost", emptyMap(), maxMessages = 200)

        assertNotNull(merge)
        assertEquals(
            listOf("welcome", "a", "lost", "b", "c", "back", "d"),
            merge!!.messages.map { it.id },
        )
        assertEquals(listOf("b", "c"), merge.added.map { it.id })
    }

    @Test
    fun nothingMissedLeavesTheChat() {
        val messages = listOf(
            testChatMessage(id = "a", timestampMillis = 100L),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
        )
        val recent = listOf(testChatMessage(id = "a", timestampMillis = 100L))

        assertNull(mergeReconnectGap(messages, recent, "lost", emptyMap(), maxMessages = 200))
    }

    @Test
    fun goneDisconnectNoticeLeavesTheChat() {
        val messages = listOf(testChatMessage(id = "a", timestampMillis = 100L))
        val recent = listOf(testChatMessage(id = "b", timestampMillis = 200L))

        assertNull(mergeReconnectGap(messages, recent, "lost", emptyMap(), maxMessages = 200))
    }

    @Test
    fun chatterWhoWroteBeforeTheOutageIsNotFirstAgain() {
        val messages = listOf(
            testChatMessage(id = "a", login = "viewer", timestampMillis = 100L),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
        )
        val recent = listOf(
            testChatMessage(id = "b", login = "viewer", timestampMillis = 200L),
            testChatMessage(id = "c", login = "newcomer", timestampMillis = 300L),
        )

        val merge = mergeReconnectGap(messages, recent, "lost", mapOf("viewer" to "viewer"), maxMessages = 200)!!

        assertEquals(listOf(false, true), merge.added.map { it.firstInSession })
        assertEquals(setOf("viewer", "newcomer"), merge.chatters.keys)
    }

    @Test
    fun firstMarkMovesFromALaterLiveMessageToTheGap() {
        val messages = listOf(
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
            testChatMessage(id = "live", login = "newcomer", timestampMillis = 400L).copy(firstInSession = true),
        )
        val recent = listOf(testChatMessage(id = "missed", login = "newcomer", timestampMillis = 300L))

        val merge = mergeReconnectGap(
            messages,
            recent,
            "lost",
            mapOf("newcomer" to "newcomer"),
            maxMessages = 200,
        )!!

        val byId = merge.messages.associateBy { it.id }
        assertTrue(byId.getValue("missed").firstInSession)
        assertFalse(byId.getValue("live").firstInSession)
    }

    @Test
    fun mergedChatKeepsTheLimit() {
        val messages = listOf(
            testChatMessage(id = "a", timestampMillis = 100L),
            notice(ChatNotice.Disconnected, "lost"),
            notice(ChatNotice.Connected, "back"),
        )
        val recent = listOf(testChatMessage(id = "b", timestampMillis = 200L))

        val merge = mergeReconnectGap(messages, recent, "lost", emptyMap(), maxMessages = 3)!!

        assertEquals(listOf("lost", "b", "back"), merge.messages.map { it.id })
    }
}
