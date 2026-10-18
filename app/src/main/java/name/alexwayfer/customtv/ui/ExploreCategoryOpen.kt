package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.data.TwitchCategory

/** A category for Explore to open: one picked in the app, or with [fromPlayer] the one under the player. */
internal data class ExploreCategoryOpen(val category: TwitchCategory, val fromPlayer: Boolean = false)

/**
 * Back from a category opened from the player expands that player again while it waits in the corner; otherwise, or
 * once the player is gone, it closes the category to Explore's tabs.
 */
internal fun exploreCategoryBackExpandsPlayer(fromPlayer: Boolean, playerMinimized: Boolean): Boolean =
    fromPlayer && playerMinimized

/**
 * Once the player expands over the category Back left in Explore, the section under it is the one it covered when the
 * category opened, [returnSection]. A section changed meanwhile stays.
 */
internal fun sectionAfterPlayerExpandsOverCategory(section: AppSection, returnSection: AppSection): AppSection =
    if (section == AppSection.Explore) returnSection else section
