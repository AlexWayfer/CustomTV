package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionArrivalTest {
    @Test
    fun theFirstChatFrameIsTheBaselineNotAnArrival() {
        assertTrue(mentionBaselineResets(baselineGeneration = null, timelineGeneration = 0))
    }

    @Test
    fun messagesArrivingWhilePlayingKeepTheBaseline() {
        assertFalse(mentionBaselineResets(baselineGeneration = 2, timelineGeneration = 2))
    }

    @Test
    fun aSeekOrLoadedReplayMakesTheShownChatTheNewBaseline() {
        assertTrue(mentionBaselineResets(baselineGeneration = 2, timelineGeneration = 3))
    }

    @Test
    fun onlyANewlyArrivedMentionAsksForFeedback() {
        val seen = chat("old", "hey alex")
        val fresh = chat("new", "hey alex")

        assertFalse(newMentionArrived(setOf("old"), listOf(seen), "alex", "Alex"))
        assertTrue(newMentionArrived(setOf("old"), listOf(seen, fresh), "alex", "Alex"))
    }

    private fun chat(id: String, text: String) = ChatMessage(
        id = id,
        userLogin = id,
        displayName = id,
        color = Color.Unspecified,
        rawText = text,
        parts = emptyList(),
        timestampMillis = 0L,
    )
}
