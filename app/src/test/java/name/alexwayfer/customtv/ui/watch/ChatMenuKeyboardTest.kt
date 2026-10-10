package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMenuKeyboardTest {
    @Test
    fun anOpenKeyboardDoesNotOpenTheMenuYet() {
        assertFalse(chatMenuOpensNow(320))
    }

    @Test
    fun aClosedKeyboardOpensTheMenuNow() {
        assertTrue(chatMenuOpensNow(0))
    }

    @Test
    fun aKeyboardThatStartsClosingClearsFocusWhileItIsStillVisible() {
        assertTrue(inputFocusClearsWhenImeStartsHiding(imeBottomPx = 400, imeTargetBottomPx = 0))
        assertFalse(inputFocusClearsWhenImeStartsHiding(imeBottomPx = 400, imeTargetBottomPx = 400))
        assertFalse(inputFocusClearsWhenImeStartsHiding(imeBottomPx = 0, imeTargetBottomPx = 0))
        assertFalse(inputFocusClearsWhenImeStartsHiding(imeBottomPx = 80, imeTargetBottomPx = 400))
    }

    @Test
    fun theEmotePickerHidesTheKeyboardAndTheMessageFieldKeepsFocus() {
        assertFalse(
            inputFocusClearsWhenImeStartsHiding(
                imeBottomPx = 400,
                imeTargetBottomPx = 0,
                keepFocus = true,
            ),
        )
    }

    @Test
    fun aKeyboardReturningAfterThePickerFinishesOnlyWhenItReachesItsTarget() {
        assertFalse(keyboardReturnFinished(imeBottomPx = 0, imeTargetBottomPx = 0))
        assertFalse(keyboardReturnFinished(imeBottomPx = 120, imeTargetBottomPx = 800))
        assertTrue(keyboardReturnFinished(imeBottomPx = 800, imeTargetBottomPx = 800))
    }

    @Test
    fun aClosingKeyboardReleasesTheComposerAsSoonAsItStartsToClose() {
        assertTrue(chatComposerOpen(imeTargetBottomPx = 800, pickerOpen = false, keyboardReturning = false))
        assertFalse(chatComposerOpen(imeTargetBottomPx = 0, pickerOpen = false, keyboardReturning = false))
    }

    @Test
    fun switchingBetweenThePickerAndTheKeyboardKeepsTheComposerOpen() {
        assertTrue(chatComposerOpen(imeTargetBottomPx = 0, pickerOpen = true, keyboardReturning = false))
        assertTrue(chatComposerOpen(imeTargetBottomPx = 0, pickerOpen = false, keyboardReturning = true))
    }

    @Test
    fun aLoggedOutChatFieldOpensLoginAndAFieldThatCanSendDoesNot() {
        assertTrue(loggedOutChatFieldOpensLogin(canSend = false))
        assertFalse(loggedOutChatFieldOpensLogin(canSend = true))
    }

    @Test
    fun withTheKeyboardUpAndNoMessageTheBadgeAndTheMenuGiveWay() {
        assertEquals(ChatFieldSideButtons(start = false, endButton = false), chatFieldSideButtons(composerOpen = true, hasMessage = false, sending = false, fullscreen = false))
    }

    @Test
    fun aTypedMessageBringsTheEndButtonBackAsSendWithTheBadgeStillAway() {
        assertEquals(ChatFieldSideButtons(start = false, endButton = true), chatFieldSideButtons(composerOpen = true, hasMessage = true, sending = false, fullscreen = false))
    }

    @Test
    fun aMessageBeingSentKeepsItsProgressInView() {
        assertTrue(chatFieldSideButtons(composerOpen = true, hasMessage = false, sending = true, fullscreen = false).endButton)
    }

    @Test
    fun withTheKeyboardDownBothButtonsShow() {
        assertEquals(ChatFieldSideButtons(start = true, endButton = true), chatFieldSideButtons(composerOpen = false, hasMessage = false, sending = false, fullscreen = false))
    }

    @Test
    fun besideAFullScreenPlayerTheClosedFieldShowsAlone() {
        assertEquals(ChatFieldSideButtons(start = false, endButton = false), chatFieldSideButtons(composerOpen = false, hasMessage = false, sending = false, fullscreen = true))
    }

    @Test
    fun besideAFullScreenPlayerTheKeyboardBringsBothButtons() {
        assertEquals(ChatFieldSideButtons(start = true, endButton = true), chatFieldSideButtons(composerOpen = true, hasMessage = false, sending = false, fullscreen = true))
    }

    @Test
    fun besideAFullScreenPlayerAMessageBeingSentKeepsItsProgressInView() {
        assertTrue(chatFieldSideButtons(composerOpen = false, hasMessage = false, sending = true, fullscreen = true).endButton)
    }
}
