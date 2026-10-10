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
