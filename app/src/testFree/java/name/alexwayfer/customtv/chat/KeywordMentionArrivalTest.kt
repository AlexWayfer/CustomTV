package name.alexwayfer.customtv.chat

import androidx.compose.ui.graphics.Color
import name.alexwayfer.customtv.data.KeywordPhrase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMentionArrivalTest {
    private val phrases = listOf(KeywordPhrase("raid", wholeWord = true))

    @Test
    fun aKeywordAsksForNoFeedbackInTheFreeBuild() {
        assertFalse(newMentionArrived(setOf("old"), listOf(chat("new", "raid")), "alex", "Alex", phrases))
    }

    @Test
    fun aMentionStillAsksForFeedbackWithSavedKeywords() {
        assertTrue(newMentionArrived(setOf("old"), listOf(chat("new", "hey alex")), "alex", "Alex", phrases))
    }

    private fun chat(id: String, text: String) =
        testChatMessage(id, text, login = id, color = Color.Unspecified, parts = emptyList(), timestampMillis = 0L)
}
