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
        assertEquals(ChatFieldSideButtons(start = false, endButton = false), sideButtons(composerOpen = true))
    }

    @Test
    fun aTypedMessageBringsTheEndButtonBackAsSendWithTheBadgeStillAway() {
        assertEquals(
            ChatFieldSideButtons(start = false, endButton = true),
            sideButtons(composerOpen = true, hasMessage = true),
        )
    }

    @Test
    fun aMessageBeingSentKeepsItsProgressInView() {
        assertTrue(sideButtons(composerOpen = true, sending = true).endButton)
    }

    @Test
    fun withTheKeyboardDownBothButtonsShow() {
        assertEquals(ChatFieldSideButtons(start = true, endButton = true), sideButtons())
    }

    @Test
    fun besideAFullScreenPlayerTheClosedFieldShowsAlone() {
        assertEquals(ChatFieldSideButtons(start = false, endButton = false), sideButtons(fullscreen = true))
    }

    @Test
    fun besideAFullScreenPlayerTheKeyboardBringsBothButtons() {
        assertEquals(
            ChatFieldSideButtons(start = true, endButton = true),
            sideButtons(composerOpen = true, fullscreen = true),
        )
    }

    @Test
    fun besideAFullScreenPlayerAMessageBeingSentKeepsItsProgressInView() {
        assertTrue(sideButtons(sending = true, fullscreen = true).endButton)
    }

    private fun sideButtons(
        composerOpen: Boolean = false,
        hasMessage: Boolean = false,
        sending: Boolean = false,
        fullscreen: Boolean = false,
    ) = chatFieldSideButtons(
        composerOpen = composerOpen,
        hasMessage = hasMessage,
        sending = sending,
        fullscreen = fullscreen,
    )
}
