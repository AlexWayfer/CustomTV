package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardMessageDeduperTest {
    @Test
    fun ignoresPubSubChatLineUntilIrcArrives() {
        val pubSub = rewardMessage(
            id = "reward-redemption-1",
            text = "coin",
            color = Color(0xFF00FF00),
            title = "АТАКА ГИГАНТОВ",
            prompt = "FFZ:GE",
        )
        val afterPubSub = RewardMessageDeduper.apply(emptyList(), pubSub, maxMessages = 50)
        assertTrue(afterPubSub.messages.isEmpty())
        assertEquals(1, afterPubSub.pending.size)

        val irc = rewardMessage(
            id = "irc-uuid",
            text = "coin",
            color = Color(0xFF8A2BE2),
            badges = listOf(ChatBadge("broadcaster", "1")),
            title = "",
            timestampMillis = pubSub.timestampMillis + 250,
        )
        val afterIrc = RewardMessageDeduper.apply(
            afterPubSub.messages,
            irc,
            maxMessages = 50,
            pending = afterPubSub.pending,
        )
        assertEquals(1, afterIrc.messages.size)
        assertTrue(afterIrc.pending.isEmpty())
        val kept = afterIrc.messages.single()
        assertEquals("irc-uuid", kept.id)
        assertEquals(Color(0xFF8A2BE2), kept.color)
        assertEquals("broadcaster", kept.badges.single().setId)
        assertEquals("АТАКА ГИГАНТОВ", kept.reward?.title)
        assertEquals("FFZ:GE", kept.reward?.prompt)
        assertEquals("irc-uuid", afterIrc.colorSource?.id)
    }

    @Test
    fun pubSubOnlyEnrichesExistingIrcAndDoesNotReplaceIt() {
        val irc = rewardMessage(
            id = "irc-uuid",
            text = "coin",
            color = Color(0xFFFF4500),
            badges = listOf(ChatBadge("moderator", "1")),
            title = "",
        )
        val pubSub = rewardMessage(
            id = "reward-redemption-1",
            text = "coin",
            color = Color(0xFF00FF00),
            title = "АТАКА ГИГАНТОВ",
            timestampMillis = irc.timestampMillis + 400,
        )
        val result = RewardMessageDeduper.apply(listOf(irc), pubSub, maxMessages = 50)
        assertEquals(1, result.messages.size)
        assertTrue(result.pending.isEmpty())
        assertEquals("irc-uuid", result.messages[0].id)
        assertEquals(Color(0xFFFF4500), result.messages[0].color)
        assertEquals("moderator", result.messages[0].badges.single().setId)
        assertEquals("АТАКА ГИГАНТОВ", result.messages[0].reward?.title)
    }

    @Test
    fun stillShowsPubSubWhenThereIsNoChatText() {
        val pubSub = rewardMessage(
            id = "reward-redemption-1",
            text = "",
            color = Color(0xFF00FF00),
            title = "Posture Check",
        )
        val result = RewardMessageDeduper.apply(emptyList(), pubSub, maxMessages = 50)
        assertEquals(1, result.messages.size)
        assertEquals("reward-redemption-1", result.messages.single().id)
        assertTrue(result.pending.isEmpty())
    }

    @Test
    fun keepsSeparateRedeemsWithDifferentText() {
        val first = rewardMessage(id = "irc-1", text = "Kappa")
        val second = rewardMessage(id = "irc-2", text = "Pog")
        val result = RewardMessageDeduper.apply(listOf(first), second, maxMessages = 50)
        assertEquals(2, result.messages.size)
        assertEquals("irc-2", result.colorSource?.id)
    }

    @Test
    fun aLiveMessageTheHistoryAlreadyShowsIsNotAddedAgain() {
        val fromHistory = rewardMessage(id = "irc-1", text = "Kappa").copy(eventKind = ChatEventKind.Normal, reward = null)
        val result = RewardMessageDeduper.apply(listOf(fromHistory), fromHistory, maxMessages = 50)
        assertEquals(listOf(fromHistory), result.messages)
        assertEquals(null, result.colorSource)
    }

    @Test
    fun aLiveRewardTheHistoryAlreadyShowsIsNotAddedAgain() {
        val fromHistory = rewardMessage(id = "irc-1", text = "Kappa")
        val result = RewardMessageDeduper.apply(listOf(fromHistory), fromHistory, maxMessages = 50)
        assertEquals(listOf(fromHistory), result.messages)
    }

    private fun rewardMessage(
        id: String,
        text: String,
        color: Color = Color.White,
        badges: List<ChatBadge> = emptyList(),
        title: String = "Giant",
        prompt: String? = null,
        timestampMillis: Long = 1_700_000_000_000L,
    ): ChatMessage {
        return ChatMessage(
            id = id,
            userLogin = "alexwayfer",
            displayName = "AlexWayfer",
            color = color,
            rawText = text,
            parts = if (text.isBlank()) emptyList() else listOf(ChatPart.Text(text)),
            timestampMillis = timestampMillis,
            badges = badges,
            eventKind = ChatEventKind.Reward,
            reward = ChatReward(
                id = "giants",
                title = title,
                cost = 10_000,
                prompt = prompt,
            ),
        )
    }
}
