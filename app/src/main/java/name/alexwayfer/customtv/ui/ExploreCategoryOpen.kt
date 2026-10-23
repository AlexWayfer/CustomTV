package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.data.TwitchCategory

/** Where a category opened in Explore came from: Explore's own lists and search, the player, or the settings. */
internal enum class ExploreCategoryFrom { Explore, Player, Settings }

/** A category for Explore to open, and where it was opened [from]. */
internal data class ExploreCategoryOpen(
    val category: TwitchCategory,
    val from: ExploreCategoryFrom = ExploreCategoryFrom.Explore,
)

/** What Back from an open category does. */
internal enum class ExploreCategoryBack { ExpandPlayer, Settings, Tabs }

/**
 * Back from a category opened from the player expands that player again while it waits in the corner, and from one
 * opened from the settings returns to them; otherwise, or once the player is gone, it closes the category to
 * Explore's tabs.
 */
internal fun exploreCategoryBack(from: ExploreCategoryFrom, playerMinimized: Boolean): ExploreCategoryBack =
    when (from) {
        ExploreCategoryFrom.Player if playerMinimized -> ExploreCategoryBack.ExpandPlayer
        ExploreCategoryFrom.Settings -> ExploreCategoryBack.Settings
        else -> ExploreCategoryBack.Tabs
    }

/**
 * Once the player expands over the category Back left in Explore, the section under it is the one it covered when the
 * category opened, [returnSection]. A section changed meanwhile stays.
 */
internal fun sectionAfterPlayerExpandsOverCategory(section: AppSection, returnSection: AppSection): AppSection =
    if (section == AppSection.Explore) returnSection else section
