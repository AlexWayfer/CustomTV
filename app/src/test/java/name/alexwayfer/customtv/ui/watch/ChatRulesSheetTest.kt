package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRulesSheetTest {
    private val rules = listOf("Be nice")

    private fun needsConfirmation(
        canSend: Boolean = true,
        rules: List<String>? = this.rules,
        rulesFingerprint: String? = "a",
        confirmationLoaded: Boolean = true,
        acknowledgedFingerprint: String? = null,
    ) = chatRulesNeedConfirmation(canSend, rules, rulesFingerprint, confirmationLoaded, acknowledgedFingerprint)

    @Test
    fun aSenderInAChannelWithUnconfirmedRulesSeesThemFirst() {
        assertTrue(needsConfirmation())
    }

    @Test
    fun confirmedRulesAreNotShownAgain() {
        assertFalse(needsConfirmation(acknowledgedFingerprint = "a"))
    }

    @Test
    fun editedRulesShowAgainAfterAnEarlierConfirmation() {
        assertTrue(needsConfirmation(rulesFingerprint = "b", acknowledgedFingerprint = "a"))
    }

    @Test
    fun rulesWaitUntilTheStoredConfirmationIsRead() {
        assertFalse(needsConfirmation(confirmationLoaded = false))
    }

    @Test
    fun aChannelWithoutRulesOrWithRulesStillLoadingSendsAtOnce() {
        assertFalse(needsConfirmation(rules = emptyList()))
        assertFalse(needsConfirmation(rules = null, rulesFingerprint = null))
    }

    @Test
    fun aLoggedOutUserLogsInInsteadOfSeeingRules() {
        assertFalse(needsConfirmation(canSend = false))
    }

    @Test
    fun confirmingRulesFromThePickerButtonOpensThePicker() {
        assertEquals(ChatRulesFollowUp.EmotePicker, chatRulesFollowUp(ChatRulesTrigger.EmotePicker))
    }

    @Test
    fun confirmingRulesFromTheFieldOrASendReturnsToTheKeyboard() {
        assertEquals(ChatRulesFollowUp.Keyboard, chatRulesFollowUp(ChatRulesTrigger.MessageField))
        assertEquals(ChatRulesFollowUp.Keyboard, chatRulesFollowUp(ChatRulesTrigger.Send))
    }

    @Test
    fun confirmingRulesOpenedFromChatSettingsJustClosesThem() {
        assertEquals(ChatRulesFollowUp.Nothing, chatRulesFollowUp(ChatRulesTrigger.Settings))
    }
}
