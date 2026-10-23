package name.alexwayfer.customtv.ui.watch

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatDraftEmoteRangesTest {
    private val emotes = setOf("KEKW", "Kappa")

    @Test
    fun theDraftSplitsIntoWordsOnWhitespace() {
        assertEquals(
            listOf(TextRange(0, 4), TextRange(5, 7), TextRange(8, 13)),
            chatDraftWords("KEKW hi Kappa"),
        )
    }

    @Test
    fun repeatedSpacesDoNotShiftTheWords() {
        assertEquals(listOf(TextRange(0, 1), TextRange(3, 7)), chatDraftWords("a  KEKW  "))
    }

    @Test
    fun anEmptyDraftHasNoWords() {
        assertEquals(emptyList<TextRange>(), chatDraftWords(""))
    }

    @Test
    fun theSpacesAroundAnEmoteAreItsGaps() {
        assertEquals(mapOf(2 to 1, 7 to 1), chatDraftEmoteGaps("so KEKW ok", listOf(TextRange(3, 7))))
    }

    @Test
    fun aSpaceBetweenTwoEmotesTouchesTwoPlaques() {
        assertEquals(
            mapOf(4 to 2),
            chatDraftEmoteGaps("KEKW Kappa", listOf(TextRange(0, 4), TextRange(5, 10))),
        )
    }

    @Test
    fun anEmoteAtTheEdgesOfTheDraftHasNoGapThere() {
        assertEquals(emptyMap<Int, Int>(), chatDraftEmoteGaps("KEKW", listOf(TextRange(0, 4))))
    }

    @Test
    fun aGapGetsAStepPerPlaqueAndOneMoreSoEveryGapLooksAlike() {
        assertEquals(2, chatDraftEmoteGapSteps(plaques = 1, lastCharacter = false))
        assertEquals(3, chatDraftEmoteGapSteps(plaques = 2, lastCharacter = false))
    }

    @Test
    fun aSpaceTypedAfterAnEmoteAtTheEndGetsDoubleSteps() {
        assertEquals(4, chatDraftEmoteGapSteps(plaques = 1, lastCharacter = true))
    }

    @Test
    fun anEmoteAfterAWordHasTwoStepsBeforeIt() {
        assertEquals(2, chatDraftEmoteGapStepsBefore(TextRange(3, 7), mapOf(2 to 1, 7 to 1)))
    }

    @Test
    fun anEmoteAfterAnotherEmoteHasThreeStepsBeforeIt() {
        assertEquals(3, chatDraftEmoteGapStepsBefore(TextRange(5, 10), mapOf(4 to 2)))
    }

    @Test
    fun anEmoteAtTheStartHasNoStepsBeforeIt() {
        assertEquals(0, chatDraftEmoteGapStepsBefore(TextRange(0, 4), mapOf(4 to 2)))
    }

    @Test
    fun anEmoteTheUserCanSendIsACodeAndAnotherCaseIsNot() {
        assertTrue(chatDraftIsEmoteCode("KEKW", emotes, bttvModifiers = false, ffzModifiers = false))
        assertFalse(chatDraftIsEmoteCode("kekw", emotes, bttvModifiers = false, ffzModifiers = false))
    }

    @Test
    fun aBttvModifierIsACodeOnlyWithBttvOn() {
        assertTrue(chatDraftIsEmoteCode("w!", emotes, bttvModifiers = true, ffzModifiers = false))
        assertFalse(chatDraftIsEmoteCode("w!", emotes, bttvModifiers = false, ffzModifiers = true))
    }

    @Test
    fun anFfzModifierIsACodeOnlyWithFfzOn() {
        assertTrue(chatDraftIsEmoteCode("ffzW", emotes, bttvModifiers = false, ffzModifiers = true))
        assertFalse(chatDraftIsEmoteCode("ffzW", emotes, bttvModifiers = true, ffzModifiers = false))
    }
}
