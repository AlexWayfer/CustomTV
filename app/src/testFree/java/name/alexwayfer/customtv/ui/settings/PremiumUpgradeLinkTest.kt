package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.telegram.TelegramGroupState
import name.alexwayfer.customtv.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumUpgradeLinkTest {
    private val topic = "https://t.me/c/2345678901/5"
    private val purchase = PREMIUM_SWITCH_URL_USD

    @Test
    fun anAccountWithAccessOpensThePremiumTopic() {
        assertEquals(topic, premiumUpgradeLink(TelegramGroupState.Open(null), topic, purchase))
    }

    @Test
    fun anAccountWithoutAccessOpensThePurchase() {
        assertEquals(purchase, premiumUpgradeLink(TelegramGroupState.Unavailable, topic, purchase))
        assertEquals(purchase, premiumUpgradeLink(TelegramGroupState.Failed(UiText.Raw("Timeout")), topic, purchase))
    }

    @Test
    fun anUnfinishedCheckOpensThePurchase() {
        assertEquals(purchase, premiumUpgradeLink(TelegramGroupState.Checking, topic, purchase))
    }

    @Test
    fun aMissingTopicLinkOpensThePurchase() {
        assertEquals(purchase, premiumUpgradeLink(TelegramGroupState.Open(null), null, purchase))
    }
}
