package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.telegram.TelegramGroupState

/**
 * Where Upgrade to Premium leads: an account that already opens the Premium group goes to its topic
 * to download the build; anyone else, or before the check answers, goes to the purchase.
 */
internal fun premiumUpgradeLink(
    premiumGroup: TelegramGroupState,
    premiumTopicLink: String?,
    purchaseLink: String,
): String =
    if (premiumGroup is TelegramGroupState.Open && premiumTopicLink != null) premiumTopicLink else purchaseLink
