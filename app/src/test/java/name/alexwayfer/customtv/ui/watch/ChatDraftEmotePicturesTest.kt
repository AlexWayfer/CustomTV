package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.TextRange
import name.alexwayfer.customtv.chat.PickerEmote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChatDraftEmotePicturesTest {
    private val kappa = PickerEmote("Kappa", "file:///missing/kappa.png")
    private val kekw = PickerEmote("KEKW", "file:///missing/kekw.png")
    private val pictures = mapOf("Kappa" to kappa, "KEKW" to kekw)
    private val isEmote: (String) -> Boolean = { it in pictures || it == "w!" }

    private fun shown(text: String) = chatDraftShownEmotes(text, isEmote, { pictures[it] }) { null }

    @Test
    fun aPictureInsideTheLineWidensItsCharacterByTheRestOfItsRoom() {
        assertEquals(1.5f, chatDraftPictureSpacingEm(slotPx = 40f, glyphPx = 16f, fontSizePx = 16f, startsLine = false))
    }

    @Test
    fun aPictureThatStartsTheLineTakesTwiceTheSpacingAsAndroidDropsTheHalfBeforeIt() {
        assertEquals(3f, chatDraftPictureSpacingEm(slotPx = 40f, glyphPx = 16f, fontSizePx = 16f, startsLine = true))
    }

    @Test
    fun anEmoteFollowedByASpaceBecomesOneCharacterPicture() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(3, 8), TextRange(3, 4), kappa)),
            shown("so Kappa ok"),
        )
    }

    @Test
    fun anEmoteAtTheStartFollowedByASpaceBecomesAPicture() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(0, 5), TextRange(0, 1), kappa)),
            shown("Kappa "),
        )
    }

    @Test
    fun aCodeInsideALongerWordStaysText() {
        assertEquals(emptyList<ChatDraftShownEmote>(), shown("Kappaoh "))
    }

    @Test
    fun anEmoteBeingTypedAtTheEndStaysTextOnAPlaque() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(3, 8), TextRange(3, 8), null)),
            shown("so Kappa"),
        )
    }

    @Test
    fun aModifierWithoutAPictureStaysTextEvenBeforeASpace() {
        assertEquals(
            listOf(ChatDraftShownEmote(TextRange(0, 2), TextRange(0, 2), null)),
            shown("w! "),
        )
    }

    @Test
    fun earlierPicturesShiftTheShownRangesAfterThem() {
        assertEquals(
            listOf(
                ChatDraftShownEmote(TextRange(0, 4), TextRange(0, 1), kekw),
                ChatDraftShownEmote(TextRange(5, 10), TextRange(2, 3), kappa),
                ChatDraftShownEmote(TextRange(11, 15), TextRange(4, 8), null),
            ),
            shown("KEKW Kappa KEKW"),
        )
    }

    @Test
    fun theShownTextIsShorterByEveryCollapsedCode() {
        val text = "KEKW Kappa KEKW"
        assertEquals(8, chatDraftShownLength(text.length, shown(text)))
    }

    @Test
    fun withoutAScreenReaderTheFieldShowsPictures() {
        val metrics = ChatDraftEmoteMetrics(glyphWidthPx = 10f, pictureHeightPx = 24f, fontSizePx = 16f)
        assertSame(metrics, chatDraftPictureMetrics(metrics, screenReaderOn = false))
    }

    @Test
    fun aScreenReaderKeepsTheCodesText() {
        val metrics = ChatDraftEmoteMetrics(glyphWidthPx = 10f, pictureHeightPx = 24f, fontSizePx = 16f)
        assertNull(chatDraftPictureMetrics(metrics, screenReaderOn = true))
    }

    @Test
    fun withoutPicturesTheShownTextKeepsItsLength() {
        val text = "so Kappa"
        assertEquals(text.length, chatDraftShownLength(text.length, shown(text)))
    }
}
