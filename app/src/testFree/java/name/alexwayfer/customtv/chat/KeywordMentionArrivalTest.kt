package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.KeywordPhrase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMentionArrivalTest {
    private val self = ChatMentionSelf("alex", "Alex", listOf(KeywordPhrase("raid", wholeWord = true)))

    @Test
    fun aKeywordAsksForNoFeedbackInTheFreeBuild() {
        assertFalse(newMentionArrived(setOf("old"), listOf(newChat("raid")), self))
    }

    @Test
    fun aMentionStillAsksForFeedbackWithSavedKeywords() {
        assertTrue(newMentionArrived(setOf("old"), listOf(newChat("hey alex")), self))
    }

    /** A message that is not among the seen ones. */
    private fun newChat(text: String) =
        testChatMessage(
            "new",
            text,
            login = "new",
            color = Color.Unspecified,
            parts = emptyList(),
            timestampMillis = 0L,
        )
}
