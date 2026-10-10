package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatConnectionNoticeTest {
    @Test
    fun welcomeReplacesInitialConnectingNotice() {
        val messages = listOf(chatNoticeMessage(ChatNotice.Connecting, "initial"))

        val result = replaceOrAppendWelcomeNotice(messages, "initial", maxMessages = 200)

        assertEquals(1, result.size)
        assertEquals(ChatNotice.Welcome, result.single().notice)
        assertEquals("initial", result.single().id)
    }

    @Test
    fun welcomeIsAppendedAndLimitedWhenInitialNoticeIsGone() {
        val messages = listOf(
            chatNoticeMessage(ChatNotice.Disconnected, "old"),
            chatNoticeMessage(ChatNotice.Connecting, "new"),
        )

        val result = replaceOrAppendWelcomeNotice(messages, "initial", maxMessages = 2)

        assertEquals(listOf("new", "initial"), result.map { it.id })
        assertEquals(ChatNotice.Welcome, result.last().notice)
    }

    @Test
    fun reconnectingAfterDisconnectedAppendsNewNotice() {
        val messages = listOf(
            chatNoticeMessage(ChatNotice.Welcome, "welcome"),
            chatNoticeMessage(ChatNotice.Disconnected, "disc-1"),
        )
        val result = replaceOrAppendReconnectNotice(
            messages,
            ChatNotice.Reconnecting,
            newId = "reconnect-2",
            maxMessages = 200,
        )
        assertEquals(3, result.size)
        assertEquals(ChatNotice.Welcome, result[0].notice)
        assertEquals(ChatNotice.Disconnected, result[1].notice)
        assertEquals(ChatNotice.Reconnecting, result[2].notice)
        assertEquals("reconnect-2", result[2].id)
    }

    @Test
    fun connectedReplacesTrailingReconnecting() {
        val messages = listOf(
            chatNoticeMessage(ChatNotice.Disconnected, "disc-1"),
            chatNoticeMessage(ChatNotice.Reconnecting, "reconnect-2"),
        )
        val result = replaceOrAppendReconnectNotice(
            messages,
            ChatNotice.Connected,
            newId = "unused",
            maxMessages = 200,
        )
        assertEquals(2, result.size)
        assertEquals(ChatNotice.Disconnected, result[0].notice)
        assertEquals(ChatNotice.Connected, result[1].notice)
        assertEquals("reconnect-2", result[1].id)
    }

    @Test
    fun laterReconnectDoesNotRewriteEarlierConnected() {
        val chat = ChatMessage(
            id = "msg-1",
            userLogin = "user",
            displayName = "User",
            color = androidx.compose.ui.graphics.Color.White,
            rawText = "hi",
            parts = listOf(ChatPart.Text("hi")),
            timestampMillis = 1L,
        )
        val messages = listOf(
            chatNoticeMessage(ChatNotice.Connected, "reconnect-1"),
            chat,
            chatNoticeMessage(ChatNotice.Disconnected, "disc-2"),
        )
        val reconnecting = replaceOrAppendReconnectNotice(
            messages,
            ChatNotice.Reconnecting,
            newId = "reconnect-2",
            maxMessages = 200,
        )
        val connected = replaceOrAppendReconnectNotice(
            reconnecting,
            ChatNotice.Connected,
            newId = "unused",
            maxMessages = 200,
        )
        assertEquals(ChatNotice.Connected, connected[0].notice)
        assertEquals("reconnect-1", connected[0].id)
        assertEquals("hi", connected[1].rawText)
        assertEquals(ChatNotice.Disconnected, connected[2].notice)
        assertEquals(ChatNotice.Connected, connected[3].notice)
        assertEquals("reconnect-2", connected[3].id)
    }

    @Test
    fun duplicateReconnectingStaysASingleTrailingNotice() {
        val messages = listOf(
            chatNoticeMessage(ChatNotice.Disconnected, "disc-1"),
            chatNoticeMessage(ChatNotice.Reconnecting, "reconnect-1"),
        )
        val result = replaceOrAppendReconnectNotice(
            messages,
            ChatNotice.Reconnecting,
            newId = "reconnect-2",
            maxMessages = 200,
        )
        assertEquals(2, result.size)
        assertEquals(ChatNotice.Reconnecting, result[1].notice)
        assertEquals("reconnect-1", result[1].id)
    }

    @Test
    fun recentMessagesAreInsertedBeforeWelcomeAndDoNotDuplicateLiveMessages() {
        val welcome = chatNoticeMessage(ChatNotice.Welcome, "initial")
        val live = message("live", "live")
        val result = insertRecentChatBeforeWelcome(
            messages = listOf(welcome, live),
            recent = listOf(message("old", "old"), message("live", "duplicate")),
            initialNoticeId = "initial",
            maxMessages = 200,
        )
        assertEquals(listOf("old", "initial", "live"), result.map { it.id })
    }

    private fun message(id: String, text: String) = ChatMessage(
        id = id,
        userLogin = "user",
        displayName = "User",
        color = androidx.compose.ui.graphics.Color.White,
        rawText = text,
        parts = listOf(ChatPart.Text(text)),
        timestampMillis = 1L,
    )
}
