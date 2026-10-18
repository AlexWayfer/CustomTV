package name.alexwayfer.customtv.ui.watch

import name.alexwayfer.customtv.data.TwitchCategory

/**
 * The category a tap on the info panel's category line opens in Explore: one with an ID, on the panel shown whole.
 * A tap on the collapsed panel opens the panel instead, and a chapter without a game has nothing to open.
 */
internal fun panelCategoryToOpen(category: TwitchCategory?, expanded: Boolean): TwitchCategory? =
    category?.takeIf { expanded && !it.id.isNullOrBlank() }
