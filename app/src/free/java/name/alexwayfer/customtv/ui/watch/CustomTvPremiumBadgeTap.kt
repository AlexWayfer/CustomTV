package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.ui.PremiumBannerRequest

/** A chatter card's Premium badge row shows the Premium banner. */
internal val customTvPremiumBadgeTap: (() -> Unit)? = { PremiumBannerRequest.send("a chatter's Premium badge") }
