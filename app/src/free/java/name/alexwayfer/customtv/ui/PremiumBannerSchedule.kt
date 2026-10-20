package name.alexwayfer.customtv.ui

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

internal val PREMIUM_BANNER_INTERVAL = 3.days
internal const val PREMIUM_BANNER_PLAYER_OPENS = 3

// How long the banner waits after a player closes, so a stream opened right away from the list goes first.
internal val PREMIUM_BANNER_CLOSE_DELAY = 1.seconds

/**
 * The banner waits a few days and a few player opens since the last show, or since the install before the first
 * one, while a new user meets Premium only at its locked features. So someone who rarely comes back is not met with
 * it on every visit. A start in the future means the clock moved back, so the time has passed rather than waiting for
 * the clock.
 */
internal fun premiumBannerDue(
    nowMillis: Long,
    shownAtMillis: Long?,
    installedAtMillis: Long,
    playerOpens: Int,
): Boolean {
    val since = shownAtMillis ?: installedAtMillis
    val waited = nowMillis < since || nowMillis - since >= PREMIUM_BANNER_INTERVAL.inWholeMilliseconds
    return waited && playerOpens >= PREMIUM_BANNER_PLAYER_OPENS
}

/** A stream or a recording appears where none was open; switching between them is not a new open. */
internal fun premiumBannerPlayerOpened(wasPlayerOpen: Boolean, playerOpen: Boolean): Boolean =
    !wasPlayerOpen && playerOpen

/**
 * The banner comes when the last player closes back to the app: a stream or a recording, never over one. A
 * stream that gives way to a recording keeps a player open, and one closed in picture-in-picture leaves the
 * user outside the app.
 */
internal fun premiumBannerMoment(wasPlayerOpen: Boolean, playerOpen: Boolean, inPictureInPicture: Boolean): Boolean =
    wasPlayerOpen && !playerOpen && !inPictureInPicture

/**
 * Coming back to the app is a moment too, for those who leave a stream by the recents screen or picture-in-picture
 * and never close it inside the app. Only on the home screen: not over a player, such as a channel the app reopens,
 * and not in another section the app comes back to.
 */
internal fun premiumBannerReturnMoment(playerOpen: Boolean, inPictureInPicture: Boolean, onHome: Boolean): Boolean =
    onHome && !playerOpen && !inPictureInPicture
