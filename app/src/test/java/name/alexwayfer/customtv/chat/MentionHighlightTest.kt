package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionHighlightTest {
    @Test
    fun aReplyToTheSignedInUserIsAMention() {
        val message = chat("other", "sure", replyLogin = "alex")

        assertTrue(messageMentionsUser(message, "alex", "Alex"))
    }

    @Test
    fun theNameInTheTextIsAMentionWhenItIsAWholeNick() {
        assertTrue(messageMentionsUser(chat("other", "hey @Alex"), "alex", "Alex"))
        assertTrue(messageMentionsUser(chat("other", "hey Alex!"), "alexwayfer", "Alex"))
        assertFalse(messageMentionsUser(chat("other", "hey AlexWayfer"), "alex", "Alex"))
    }

    @Test
    fun theAuthorsOwnMessageIsNotAMention() {
        assertFalse(messageMentionsUser(chat("alex", "hey alex"), "alex", "Alex"))
    }

    @Test
    fun aGuestAndARaidLineAreNotMentions() {
        assertFalse(messageMentionsUser(chat("other", "hey alex"), "", "Alex"))
        assertFalse(
            messageMentionsUser(
                chat("other", "alex raided", kind = ChatEventKind.Raid),
                "alex",
                "Alex",
            ),
        )
    }

    private fun chat(
        login: String,
        text: String,
        replyLogin: String? = null,
        kind: ChatEventKind = ChatEventKind.Normal,
    ) = ChatMessage(
        id = "1",
        userLogin = login,
        displayName = login,
        color = Color.Unspecified,
        rawText = text,
        parts = emptyList(),
        timestampMillis = 0L,
        eventKind = kind,
        reply = replyLogin?.let { parent ->
            ChatReply(
                parentMsgId = "parent",
                parentUserLogin = parent,
                parentDisplayName = parent,
                parentBody = "hi",
            )
        },
    )
}
