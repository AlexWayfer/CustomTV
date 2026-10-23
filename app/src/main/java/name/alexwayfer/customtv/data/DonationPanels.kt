package name.alexwayfer.customtv.data

/** An address in text, with or without its scheme: the host, then the path up to a query or the text's end. */
private val ADDRESS = Regex("""(?i)(?:https?://)?((?:[a-z0-9-]+\.)+[a-z]{2,})(/[^\s?#()\[\]<>"]*)?""")

/** Sites where every channel page takes donations. */
private val DONATION_HOSTS = setOf(
    "donationalerts.com",
    "donatty.com",
    "donatex.gg",
    "donatepay.ru",
    "donate.stream",
    "tipeeestream.com",
    "ko-fi.com",
    "buymeacoffee.com",
    "paypal.me",
)

/** Sites where only a channel's tip page takes donations, such as streamelements.com/<channel>/tip. */
private val TIP_PAGE_HOSTS = setOf("streamelements.com", "streamlabs.com")

/**
 * The panels that link to a donation page, in their order: by the panel's link or by a link in its text. A
 * subscription site such as Boosty or Patreon is not one, nor is another page of a tip site, such as a promo.
 */
internal fun donationPanels(panels: ChannelPanels): List<ChannelPanel> =
    panels.panels.filter { panel -> listOfNotNull(panel.linkUrl, panel.description).any(::linksToDonation) }

/** Whether [text] holds an address of a donation page. */
internal fun linksToDonation(text: String): Boolean =
    ADDRESS.findAll(text).any { match ->
        isDonationPage(match.groupValues[1].lowercase(), match.groupValues[2].lowercase())
    }

private fun isDonationPage(host: String, path: String): Boolean {
    val segments = path.split('/').filter { it.isNotEmpty() }
    return segments.isNotEmpty() && when {
        DONATION_HOSTS.any(host::isSite) -> true
        host == "twitch.streamlabs.com" -> true
        TIP_PAGE_HOSTS.any(host::isSite) -> "tip" in segments
        host.isSite("paypal.com") -> "donate" in segments || "paypalme" in segments
        else -> false
    }
}

/** Whether this host is [site] or one of its subdomains, such as www. */
private fun String.isSite(site: String): Boolean = this == site || endsWith(".$site")
