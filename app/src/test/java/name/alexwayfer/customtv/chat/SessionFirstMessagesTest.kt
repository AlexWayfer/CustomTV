package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFirstMessagesTest {
    @Test
    fun marksOnlyTheFirstMessageFromEachLogin() {
        val first = rememberSessionChatter(emptyMap(), chat("viewer", "hi"))
        val second = rememberSessionChatter(first.chatters, chat("viewer", "again"))
        val other = rememberSessionChatter(second.chatters, chat("other", "hello"))
        assertTrue(first.firstInSession)
        assertFalse(second.firstInSession)
        assertTrue(other.firstInSession)
        assertEquals(setOf("viewer", "other"), other.chatters.keys)
    }

    @Test
    fun ignoresSystemAndEmoteChangeLines() {
        val notice = rememberSessionChatter(emptyMap(), chatNoticeMessage(ChatNotice.Welcome, "welcome"))
        val change = rememberSessionChatter(
            notice.chatters,
            chat("editor", "x").copy(
                eventKind = ChatEventKind.EmoteChange,
                emoteChange = ChatEmoteChange(
                    platform = EmotePlatform.SevenTv,
                    action = EmoteChangeAction.Added,
                    actorName = "editor",
                    actorColor = Color.White,
                    emoteName = "x",
                ),
            ),
        )
        val real = rememberSessionChatter(change.chatters, chat("editor", "hello"))
        assertFalse(notice.firstInSession)
        assertFalse(change.firstInSession)
        assertTrue(real.firstInSession)
        assertEquals(setOf("editor"), real.chatters.keys)
    }

    @Test
    fun aPubSubRewardWithTextDoesNotUseUpTheFirstMessage() {
        val pubsub = chat("viewer", "hello").copy(id = "reward-1", eventKind = ChatEventKind.Reward)
        val skipped = rememberSessionChatter(emptyMap(), pubsub)
        val irc = rememberSessionChatter(
            skipped.chatters,
            chat("viewer", "hello").copy(eventKind = ChatEventKind.Reward),
        )
        assertFalse(skipped.firstInSession)
        assertTrue(skipped.chatters.isEmpty())
        assertTrue(irc.firstInSession)
    }

    @Test
    fun recentChatMarksEachChattersEarliestHistoryMessage() {
        val merge = mergeRecentChatSession(
            recent = listOf(chat("viewer", "old"), chat("viewer", "older reply"), chat("other", "old")),
            current = emptyList(),
            chatters = emptyMap(),
        )
        assertEquals(listOf(true, false, true), merge.recent.map(ChatMessage::firstInSession))
        assertEquals(setOf("viewer", "other"), merge.chatters.keys)
        assertTrue(merge.movedFirstLogins.isEmpty())
    }

    @Test
    fun recentChatTakesTheFirstMarkFromALiveMessageOfTheSameChatter() {
        val live = chat("viewer", "live").copy(firstInSession = true)
        val otherLive = chat("other", "live").copy(firstInSession = true)
        val merge = mergeRecentChatSession(
            recent = listOf(chat("viewer", "old")),
            current = listOf(live, otherLive),
            chatters = mapOf("viewer" to "viewer", "other" to "other"),
        )
        assertTrue(merge.recent.single().firstInSession)
        assertEquals(listOf(false, true), merge.current.map(ChatMessage::firstInSession))
        assertEquals(setOf("viewer"), merge.movedFirstLogins)
    }

    @Test
    fun recentChatKeepsTheLiveDisplayName() {
        val merge = mergeRecentChatSession(
            recent = listOf(chat("viewer", "old").copy(displayName = "OldName")),
            current = emptyList(),
            chatters = mapOf("viewer" to "NewName"),
        )
        assertEquals("NewName", merge.chatters["viewer"])
    }

    @Test
    fun recentChatMessageAlreadyShownIsNotAddedOrMarked() {
        val shown = chat("viewer", "same").copy(firstInSession = true)
        val merge = mergeRecentChatSession(
            recent = listOf(chat("viewer", "same")),
            current = listOf(shown),
            chatters = mapOf("viewer" to "viewer"),
        )
        assertTrue(merge.recent.isEmpty())
        assertEquals(listOf(shown), merge.current)
        assertTrue(merge.movedFirstLogins.isEmpty())
    }

    private fun chat(login: String, text: String): ChatMessage {
        return ChatMessage(
            id = "$login-$text",
            userLogin = login,
            displayName = login,
            color = Color.White,
            rawText = text,
            parts = listOf(ChatPart.Text(text)),
            timestampMillis = 1L,
        )
    }
}
