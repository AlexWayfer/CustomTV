package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmoteInsertTest {
    @Test
    fun emptyFieldInsertsTheNameAndATrailingSpace() {
        val inserted = textWithInsertedEmote("", 0, 0, "Kappa", maxLength = 500)
        assertEquals(EmoteInsertion("Kappa ", 6), inserted)
    }

    @Test
    fun cursorAtTheEndGetsSpacesOnBothSides() {
        val inserted = textWithInsertedEmote("hello", 5, 5, "Kappa", maxLength = 500)
        assertEquals(EmoteInsertion("hello Kappa ", 12), inserted)
    }

    @Test
    fun theTrailingSpaceAtTheEndIsLeftOutWhenItWouldPassTheLimit() {
        val inserted = textWithInsertedEmote("hello", 5, 5, "Kappa", maxLength = 11)
        assertEquals(EmoteInsertion("hello Kappa", 11), inserted)
    }

    @Test
    fun cursorAtTheStartGetsATrailingSpaceOnly() {
        val inserted = textWithInsertedEmote("hello", 0, 0, "Kappa", maxLength = 500)
        assertEquals(EmoteInsertion("Kappa hello", 6), inserted)
    }

    @Test
    fun anExistingSpaceIsNotDoubled() {
        assertEquals(
            EmoteInsertion("hello Kappa ", 12),
            textWithInsertedEmote("hello ", 6, 6, "Kappa", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion("Kappa hello", 6),
            textWithInsertedEmote(" hello", 0, 0, "Kappa", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion("hello Kappa world", 12),
            textWithInsertedEmote("hello  world", 6, 6, "Kappa", maxLength = 500),
        )
    }

    @Test
    fun aMiddleCursorIsSurroundedAndLandsAfterTheFollowingSpace() {
        assertEquals(
            EmoteInsertion("he Kappa llo", 9),
            textWithInsertedEmote("hello", 2, 2, "Kappa", maxLength = 500),
        )
        assertEquals(
            EmoteInsertion("hello Kappa world", 12),
            textWithInsertedEmote("hello world", 6, 6, "Kappa", maxLength = 500),
        )
    }

    @Test
    fun aSelectionIsReplaced() {
        assertEquals(
            EmoteInsertion("he Kappa o", 9),
            textWithInsertedEmote("hello", 2, 4, "Kappa", maxLength = 500),
        )
    }

    @Test
    fun aNameThatWouldPassTheLimitIsNotInserted() {
        assertNull(textWithInsertedEmote("hello", 5, 5, "Kappa", maxLength = 8))
    }

    @Test
    fun pickerPanelUsesTheKeyboardHeightAndFallsBackWhenItWasNeverOpened() {
        assertEquals(48, emotePanelHeightPx(48, 0, pickerOpen = false, keyboardReturning = false, rememberedImePx = 800, fallbackPx = 400))
        assertEquals(800, emotePanelHeightPx(48, 0, pickerOpen = true, keyboardReturning = false, rememberedImePx = 800, fallbackPx = 400))
        assertEquals(400, emotePanelHeightPx(48, 0, pickerOpen = true, keyboardReturning = false, rememberedImePx = 0, fallbackPx = 400))
        assertEquals(900, emotePanelHeightPx(48, 900, pickerOpen = true, keyboardReturning = false, rememberedImePx = 800, fallbackPx = 400))
    }

    @Test
    fun onlyAKeyboardThatFinishedRisingSetsThePickerHeight() {
        assertEquals(800, settledKeyboardHeightPx(imeBottomPx = 800, imeTargetBottomPx = 800, rememberedPx = 700))
        assertEquals(700, settledKeyboardHeightPx(imeBottomPx = 300, imeTargetBottomPx = 800, rememberedPx = 700))
        assertEquals(700, settledKeyboardHeightPx(imeBottomPx = 300, imeTargetBottomPx = 0, rememberedPx = 700))
        assertEquals(700, settledKeyboardHeightPx(imeBottomPx = 0, imeTargetBottomPx = 0, rememberedPx = 700))
    }

    @Test
    fun pickerPanelIsAsLowAsALowKeyboard() {
        assertEquals(200, emotePanelHeightPx(48, 0, pickerOpen = true, keyboardReturning = false, rememberedImePx = 200, fallbackPx = 400))
    }

    @Test
    fun eachOrientationRemembersItsOwnKeyboardHeight() {
        val memory = KeyboardHeightMemory()
        assertEquals(800, memory.rise(800, landscape = false))
        assertEquals(0, memory.heightPx(imeBottomPx = 0, imeTargetBottomPx = 0, landscape = true))
        assertEquals(450, memory.rise(450, landscape = true))
        assertEquals(800, memory.heightPx(imeBottomPx = 0, imeTargetBottomPx = 0, landscape = false))
        assertEquals(450, memory.heightPx(imeBottomPx = 0, imeTargetBottomPx = 0, landscape = true))
    }

    @Test
    fun savedHeightsFillOnlyTheOrientationsNotSeenYet() {
        val memory = KeyboardHeightMemory()
        memory.useKeyboard("gboard")
        memory.rise(820, landscape = false)
        memory.restore(KeyboardHeights("gboard", portraitPx = 800, landscapePx = 450))
        assertEquals(KeyboardHeights("gboard", portraitPx = 820, landscapePx = 450), memory.heights)
    }

    @Test
    fun savedHeightsOfAnotherKeyboardAreIgnored() {
        val memory = KeyboardHeightMemory()
        memory.useKeyboard("swiftkey")
        memory.restore(KeyboardHeights("gboard", portraitPx = 800, landscapePx = 450))
        assertEquals(KeyboardHeights("swiftkey", portraitPx = 0, landscapePx = 0), memory.heights)
    }

    @Test
    fun switchingTheKeyboardBeforeThePickerForgetsTheHeights() {
        val memory = KeyboardHeightMemory()
        memory.useKeyboard("gboard")
        memory.rise(800, landscape = false)
        assertTrue(memory.useKeyboard("swiftkey"))
        assertEquals(KeyboardHeights("swiftkey", portraitPx = 0, landscapePx = 0), memory.heights)
    }

    @Test
    fun theFirstKeyboardHasNoHeightsToForget() {
        assertFalse(KeyboardHeightMemory().useKeyboard("gboard"))
    }

    @Test
    fun aNewKeyboardKeepsOnlyTheHeightItWasJustSeenWith() {
        val memory = KeyboardHeightMemory()
        memory.useKeyboard("gboard")
        memory.restore(KeyboardHeights("gboard", portraitPx = 800, landscapePx = 450))
        memory.rise(400, landscape = true)
        memory.keyboardShown("swiftkey", landscape = true)
        assertEquals(KeyboardHeights("swiftkey", portraitPx = 0, landscapePx = 400), memory.heights)
    }

    @Test
    fun aResizedKeyboardReplacesItsHeight() {
        val memory = KeyboardHeightMemory()
        memory.useKeyboard("gboard")
        memory.restore(KeyboardHeights("gboard", portraitPx = 800, landscapePx = 450))
        memory.rise(900, landscape = false)
        memory.keyboardShown("gboard", landscape = false)
        assertEquals(KeyboardHeights("gboard", portraitPx = 900, landscapePx = 450), memory.heights)
    }

    @Test
    fun aClosedPickerKeepsItsSpaceWhileTheKeyboardRisesInItsPlace() {
        assertEquals(800, emotePanelHeightPx(48, 0, pickerOpen = false, keyboardReturning = true, rememberedImePx = 800, fallbackPx = 400))
        assertEquals(800, emotePanelHeightPx(48, 300, pickerOpen = false, keyboardReturning = true, rememberedImePx = 800, fallbackPx = 400))
    }

    @Test
    fun emoteSearchPutsAColonInAnEmptyField() {
        assertEquals(EmoteInsertion(":", 1), textWithEmoteSearchColon("", 0, 0, maxLength = 500))
    }

    @Test
    fun emoteSearchAfterAWordAddsASpaceBeforeTheColon() {
        assertEquals(EmoteInsertion("hello :", 7), textWithEmoteSearchColon("hello", 5, 5, maxLength = 500))
    }

    @Test
    fun emoteSearchAfterASpaceAddsOnlyTheColon() {
        assertEquals(EmoteInsertion("hello :", 7), textWithEmoteSearchColon("hello ", 6, 6, maxLength = 500))
    }

    @Test
    fun emoteSearchBeforeAWordKeepsTheWordOutOfTheQuery() {
        assertEquals(EmoteInsertion(": hello", 1), textWithEmoteSearchColon("hello", 0, 0, maxLength = 500))
    }

    @Test
    fun emoteSearchReplacesTheSelection() {
        assertEquals(EmoteInsertion("a : b", 3), textWithEmoteSearchColon("a xyz b", 2, 5, maxLength = 500))
    }

    @Test
    fun emoteSearchInsideAColonQueryKeepsTheText() {
        assertEquals(EmoteInsertion("hi :Kap", 7), textWithEmoteSearchColon("hi :Kap", 7, 7, maxLength = 500))
    }

    @Test
    fun emoteSearchAfterABareColonKeepsTheText() {
        assertEquals(EmoteInsertion("hi :", 4), textWithEmoteSearchColon("hi :", 4, 4, maxLength = 500))
    }

    @Test
    fun emoteSearchPastTheLimitChangesNothing() {
        assertNull(textWithEmoteSearchColon("hello", 5, 5, maxLength = 6))
    }

    @Test
    fun aKeyboardStillOpenFromTheOtherOrientationDoesNotCount() {
        val memory = KeyboardHeightMemory()
        memory.rise(800, landscape = false)
        assertEquals(0, memory.heightPx(imeBottomPx = 800, imeTargetBottomPx = 800, landscape = true))
        assertEquals(450, memory.rise(450, landscape = true))
        assertEquals(800, memory.heightPx(imeBottomPx = 450, imeTargetBottomPx = 450, landscape = false))
    }

    private fun KeyboardHeightMemory.rise(px: Int, landscape: Boolean): Int {
        heightPx(imeBottomPx = px / 2, imeTargetBottomPx = px, landscape = landscape)
        return heightPx(imeBottomPx = px, imeTargetBottomPx = px, landscape = landscape)
    }
}
