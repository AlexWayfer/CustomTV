package name.alexwayfer.customtv.telegram

import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramConfigTest {
    private val free = TelegramConfig(openChatId = -1001234567890L, premiumChatId = -1002345678901L)
    private val premium = free.copy(premiumEdition = true)
    private val shown = TelegramUiState(
        openGroup = TelegramGroupState.Unavailable,
        premiumGroup = TelegramGroupState.Unavailable,
    )

    @Test
    fun checkingMarksOnlyTheEditionsGroup() {
        val freeState = shown.withEditionChecking(free)
        assertEquals(TelegramGroupState.Checking, freeState.openGroup)
        assertEquals(TelegramGroupState.Unavailable, freeState.premiumGroup)

        val premiumState = shown.withEditionChecking(premium)
        assertEquals(TelegramGroupState.Unavailable, premiumState.openGroup)
        assertEquals(TelegramGroupState.Checking, premiumState.premiumGroup)
    }

    @Test
    fun checkingWithoutAChatIsNotConfigured() {
        val state = shown.withEditionChecking(free.copy(openChatId = 0L))
        assertEquals(TelegramGroupState.NotConfigured, state.openGroup)
    }

    @Test
    fun editionGroupGoesToTheEditionsSlot() {
        val open = TelegramGroupState.Open(null)
        assertEquals(open, shown.withEditionGroup(premiumEdition = false, open).openGroup)
        assertEquals(TelegramGroupState.Unavailable, shown.withEditionGroup(premiumEdition = false, open).premiumGroup)
        assertEquals(open, shown.withEditionGroup(premiumEdition = true, open).premiumGroup)
        assertEquals(TelegramGroupState.Unavailable, shown.withEditionGroup(premiumEdition = true, open).openGroup)
    }
}
