package name.alexwayfer.customtv.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatFieldCoverTest {
    private fun cover(canSend: Boolean, focused: Boolean, rules: Boolean) =
        chatFieldCover(canSend = canSend, focused = focused, rulesNeedConfirmation = rules)

    @Test
    fun `logged out, a tap logs in, rules or not`() {
        assertEquals(ChatFieldCover.LogIn, cover(canSend = false, focused = false, rules = false))
        assertEquals(ChatFieldCover.LogIn, cover(canSend = false, focused = false, rules = true))
    }

    @Test
    fun `signed in with rules to confirm, a tap on the unfocused field opens the rules`() {
        assertEquals(ChatFieldCover.Rules, cover(canSend = true, focused = false, rules = true))
    }

    @Test
    fun `a focused field takes its own taps while rules wait for Send`() {
        assertEquals(ChatFieldCover.None, cover(canSend = true, focused = true, rules = true))
    }

    @Test
    fun `signed in with confirmed rules, nothing covers the field`() {
        assertEquals(ChatFieldCover.None, cover(canSend = true, focused = false, rules = false))
    }
}
