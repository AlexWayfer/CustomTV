package name.alexwayfer.customtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumSwitchUrlTest {
    @Test
    fun aRussianOrBelarusianSimOpensTheRublePrice() {
        assertEquals(PREMIUM_SWITCH_URL_RUB, premiumSwitchUrl("ru"))
        assertEquals(PREMIUM_SWITCH_URL_RUB, premiumSwitchUrl("by"))
    }

    @Test
    fun anUpperCaseCountryOpensTheRublePrice() {
        assertEquals(PREMIUM_SWITCH_URL_RUB, premiumSwitchUrl("RU"))
    }

    @Test
    fun aDualSimValueCountsByTheFirstSim() {
        assertEquals(PREMIUM_SWITCH_URL_RUB, premiumSwitchUrl("ru,"))
        assertEquals(PREMIUM_SWITCH_URL_USD, premiumSwitchUrl("kz,ru"))
    }

    @Test
    fun anotherCountryOpensTheDollarPrice() {
        assertEquals(PREMIUM_SWITCH_URL_USD, premiumSwitchUrl("kz"))
        assertEquals(PREMIUM_SWITCH_URL_USD, premiumSwitchUrl("us"))
    }

    @Test
    fun noSimOpensTheDollarPrice() {
        assertEquals(PREMIUM_SWITCH_URL_USD, premiumSwitchUrl(""))
        assertEquals(PREMIUM_SWITCH_URL_USD, premiumSwitchUrl(null))
    }
}
