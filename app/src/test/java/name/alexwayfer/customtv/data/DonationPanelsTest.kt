package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DonationPanelsTest {
    @Test
    fun panelLinkingToADonationPageIsADonationPanel() {
        val donation = panel("2", linkUrl = "https://www.donationalerts.com/r/ada")
        val panels = panels(
            panel("1", linkUrl = "https://boosty.to/ada"),
            donation,
            panel("3", linkUrl = "https://memealerts.com/ada"),
        )

        assertEquals(listOf(donation), donationPanels(panels))
    }

    @Test
    fun linkInThePanelTextCounts() {
        val donation = panel("1", description = "[Donatty](https://donatty.com/ada) 5% fee")

        assertEquals(listOf(donation), donationPanels(panels(donation)))
    }

    @Test
    fun everyDonationPanelIsKeptInOrder() {
        val first = panel("1", linkUrl = "https://streamelements.com/ada/tip")
        val second = panel("2", description = "Or donationalerts.com/r/ada")

        assertEquals(listOf(first, second), donationPanels(panels(first, panel("3"), second)))
    }

    @Test
    fun channelWithoutDonationPanelsHasNone() {
        assertEquals(
            emptyList<ChannelPanel>(),
            donationPanels(panels(panel("1", linkUrl = "https://www.twitch.tv/subs/ada", description = "Subscribe"))),
        )
    }

    @Test
    fun tipSiteCountsOnlyOnItsTipPage() {
        assertTrue(linksToDonation("https://streamelements.com/ada/tip"))
        assertTrue(linksToDonation("https://streamlabs.com/ada/tip"))
        assertTrue(linksToDonation("https://twitch.streamlabs.com/ada"))
        assertFalse(linksToDonation("https://streamlabs.com/goprime?promo=ada-10"))
        assertFalse(linksToDonation("https://streamelements.com/ada/store"))
    }

    @Test
    fun paypalCountsOnlyOnADonationPage() {
        assertTrue(linksToDonation("https://www.paypal.com/donate/?hosted_button_id=ABC"))
        assertTrue(linksToDonation("https://paypal.me/ada"))
        assertFalse(linksToDonation("https://www.paypal.com/signin"))
    }

    @Test
    fun siteWithoutAChannelPageIsNotADonationPage() {
        assertFalse(linksToDonation("https://www.donationalerts.com/"))
        assertFalse(linksToDonation("ko-fi.com"))
    }

    @Test
    fun anotherSiteEndingTheSameIsNotADonationPage() {
        assertFalse(linksToDonation("https://notdonationalerts.com/r/ada"))
        assertTrue(linksToDonation("HTTPS://WWW.DONATIONALERTS.COM/R/Ada"))
    }

    private fun panel(id: String, linkUrl: String? = null, description: String? = null) =
        ChannelPanel(id = id, title = null, imageUrl = null, linkUrl = linkUrl, description = description)

    private fun panels(vararg panels: ChannelPanel) =
        ChannelPanels(panels = panels.toList(), extensionCount = 0, channelDescription = null)
}
