package name.alexwayfer.customtv.ui

/** A profile, a channel's or the account's own, stays under a stream opened from it, and the stream minimizes onto it. */
internal fun sectionStaysUnderPlayer(section: AppSection): Boolean =
    section == AppSection.ChannelProfile || section == AppSection.Account

/** A covering player leaves Home behind it, except over a profile, which it returns to. */
internal fun sectionBehindOpenPlayer(section: AppSection, playerCovering: Boolean): AppSection =
    if (playerCovering && !sectionStaysUnderPlayer(section)) AppSection.Home else section

internal fun showAppShell(
    inPictureInPicture: Boolean,
    watching: Boolean,
    minimized: Boolean,
    section: AppSection,
): Boolean = !inPictureInPicture && (!watching || minimized || sectionStaysUnderPlayer(section))

/**
 * A full stream stays over the channel profile it was opened from, and the profile stays composed
 * under it. Back then minimizes the stream; the profile handles Back again once the stream is minimized.
 */
internal fun profileHandlesBack(watching: Boolean, liveMinimized: Boolean): Boolean = !watching || liveMinimized

internal fun profileBackExpandsPlayer(watching: Boolean, minimized: Boolean): Boolean =
    watching && minimized

internal sealed interface ProfileBack {
    data object ExpandPlayer : ProfileBack
    data class Show(val section: AppSection) : ProfileBack
}

/**
 * Back from a channel profile returns where it was opened: a section it was opened from comes
 * first, a profile opened from the stream expands it, and anything else goes Home.
 */
internal fun profileBack(openedFrom: AppSection?, watching: Boolean, minimized: Boolean): ProfileBack = when {
    openedFrom != null -> ProfileBack.Show(openedFrom)
    profileBackExpandsPlayer(watching, minimized) -> ProfileBack.ExpandPlayer
    else -> ProfileBack.Show(AppSection.Home)
}

/**
 * Back from a profile expands the stream over it, and the profile closes once the stream covers
 * the screen, so the stream's next Back minimizes it onto Home. A stream expanded any other way
 * keeps the section under it.
 */
internal fun sectionAfterPlayerExpands(section: AppSection, profileClosing: Boolean): AppSection =
    if (profileClosing && section == AppSection.ChannelProfile) AppSection.Home else section

internal data class ProfileVideoOpenDecision(
    val section: AppSection,
    val minimized: Boolean,
    val closeLivePlayer: Boolean,
)

internal fun profileVideoOpenDecision(
    section: AppSection,
    minimized: Boolean,
    watching: Boolean,
): ProfileVideoOpenDecision = ProfileVideoOpenDecision(
    section = section,
    minimized = minimized,
    closeLivePlayer = watching,
)
