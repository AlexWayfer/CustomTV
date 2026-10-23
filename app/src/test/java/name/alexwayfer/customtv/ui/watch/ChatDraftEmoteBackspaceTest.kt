package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import name.alexwayfer.customtv.chat.PickerEmote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatDraftEmoteBackspaceTest {
    private val pictures = mapOf(
        "Kappa" to PickerEmote("Kappa", "file:///missing/kappa.png"),
        "KEKW" to PickerEmote("KEKW", "file:///missing/kekw.png"),
    )

    private fun deletion(original: String, cursor: TextRange, edited: String): TextRange? =
        chatDraftWholeEmoteDeletion(
            original = original,
            originalSelection = cursor,
            edited = edited,
            shown = chatDraftShownEmotes(original, { it in pictures }, { pictures[it] }) { null },
        )

    @Test
    fun aBackspaceAfterAPictureAndItsSpaceTakesTheCodeToo() {
        assertEquals(TextRange(3, 8), deletion("so Kappa ", TextRange(9), "so Kappa")!!)
    }

    @Test
    fun aBackspaceBetweenTwoPicturesTakesOnlyTheOneBeforeTheSpace() {
        assertEquals(TextRange(6, 10), deletion("Kappa KEKW ", TextRange(11), "Kappa KEKW")!!)
    }

    @Test
    fun aBackspaceOfASpaceInTheMiddleTakesThePictureBeforeIt() {
        assertEquals(TextRange(0, 5), deletion("Kappa ok", TextRange(6), "Kappaok")!!)
    }

    @Test
    fun aBackspaceAfterAPlainWordDeletesOnlyTheSpace() {
        assertNull(deletion("so ok ", TextRange(6), "so ok"))
    }

    @Test
    fun aBackspaceOfALetterDeletesOnlyTheLetter() {
        assertNull(deletion("Kappa ok", TextRange(8), "Kappa o"))
    }

    @Test
    fun aDeletedSelectionIsLeftAsItIs() {
        assertNull(deletion("so Kappa ", TextRange(8, 9), "so Kappa"))
    }

    @Test
    fun aForwardDeleteOfTheSpaceIsLeftAsItIs() {
        assertNull(deletion("so Kappa ok", TextRange(8), "so Kappaok"))
    }

    @Test
    fun theFieldsBackspaceLeavesTheCursorWhereThePictureStarted() {
        val output = ChatDraftEmoteOutput({ it in pictures }, { pictures[it] }) { null }
        output.metrics = ChatDraftEmoteMetrics(glyphWidthPx = 10f, pictureHeightPx = 24f, fontSizePx = 16f)
        val state = TextFieldState("so Kappa ", TextRange(9))

        state.edit {
            replace(8, 9, "")
            with(ChatDraftEmoteBackspace(output)) { transformInput() }
        }

        assertEquals("so ", state.text.toString())
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun aCodeShownAsTextLosesOnlyTheSpace() {
        assertNull(
            chatDraftWholeEmoteDeletion(
                original = "so Kappa ",
                originalSelection = TextRange(9),
                edited = "so Kappa",
                shown = chatDraftShownEmotes("so Kappa ", { it in pictures }, { null }) { null },
            ),
        )
    }
}
