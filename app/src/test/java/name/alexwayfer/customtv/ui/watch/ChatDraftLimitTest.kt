package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatDraftLimitTest {
    private fun TextFieldState.typeLimited(text: String, maxLength: Int) = edit {
        append(text)
        with(ChatDraftLimit(maxLength)) { transformInput() }
    }

    @Test
    fun aDraftWithinTheLimitIsKept() {
        val state = TextFieldState("hello", TextRange(5))

        state.typeLimited(" wor", maxLength = 20)

        assertEquals("hello wor", state.text.toString())
        assertEquals(TextRange(9), state.selection)
    }

    @Test
    fun aDraftOverTheLimitIsCutAndItsCursorClipped() {
        val state = TextFieldState("hello", TextRange(5))

        state.typeLimited(" world", maxLength = 8)

        assertEquals("hello wo", state.text.toString())
        assertEquals(TextRange(8), state.selection)
    }

    @Test
    fun aPasteOverTheLimitKeepsItsFirstPartInsteadOfBeingDropped() {
        val state = TextFieldState()

        state.typeLimited("abcdefghij", maxLength = 4)

        assertEquals("abcd", state.text.toString())
    }
}
