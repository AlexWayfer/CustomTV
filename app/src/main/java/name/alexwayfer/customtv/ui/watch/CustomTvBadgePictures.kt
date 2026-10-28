package name.alexwayfer.customtv.ui.watch

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.BuildConfig
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ThirdPartyBadge
import name.alexwayfer.customtv.data.CustomTvBadgeEdition

@DrawableRes
internal fun customTvBadgeImage(edition: CustomTvBadgeEdition): Int = when (edition) {
    CustomTvBadgeEdition.Premium -> R.drawable.badge_custom_tv_premium
    CustomTvBadgeEdition.Free -> R.drawable.badge_custom_tv_free
}

@StringRes
internal fun customTvBadgeTitle(edition: CustomTvBadgeEdition): Int = when (edition) {
    CustomTvBadgeEdition.Premium -> R.string.custom_tv_badge_premium
    CustomTvBadgeEdition.Free -> R.string.custom_tv_badge_free
}

/**
 * What a chatter card's row of a CustomTV badge does: only the Premium one has a tap, the free build's Premium banner.
 */
internal fun customTvBadgeRowTap(edition: CustomTvBadgeEdition): (() -> Unit)? =
    if (edition == CustomTvBadgeEdition.Premium) customTvPremiumBadgeTap else null

/** Each CustomTV badge as the chat draws it: a picture of the app itself, which needs no request. */
@Composable
internal fun customTvBadgePictures(): Map<CustomTvBadgeEdition, ThirdPartyBadge> {
    val premium = stringResource(customTvBadgeTitle(CustomTvBadgeEdition.Premium))
    val free = stringResource(customTvBadgeTitle(CustomTvBadgeEdition.Free))
    return remember(premium, free) {
        mapOf(
            CustomTvBadgeEdition.Premium to ThirdPartyBadge(badgeUrl(CustomTvBadgeEdition.Premium), premium),
            CustomTvBadgeEdition.Free to ThirdPartyBadge(badgeUrl(CustomTvBadgeEdition.Free), free),
        )
    }
}

private fun badgeUrl(edition: CustomTvBadgeEdition): String =
    "android.resource://${BuildConfig.APPLICATION_ID}/${customTvBadgeImage(edition)}"
