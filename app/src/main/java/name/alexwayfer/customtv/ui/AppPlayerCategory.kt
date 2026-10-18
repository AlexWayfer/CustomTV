package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** Opens [category] in Explore as the player minimizes onto it; Back from it brings that player back. */
internal fun AppScene.openCategoryFromPlayer(category: TwitchCategory) {
    shell.categoryPlayerSection = shell.section
    shell.exploreOpenCategory = OneShotRequest(ExploreCategoryOpen(category, fromPlayer = true))
    shell.section = AppSection.Explore
}

/** Back from the player's category in Explore expands the player, which closes the category once it covers it. */
internal fun AppScene.expandPlayerOverExploreCategory() {
    shell.exploreCategoryClosesUnderPlayer = true
    shell.player = if (recordingOpen) {
        shell.player.withVideoMinimized(false)
    } else {
        shell.player.withLiveMinimized(false)
    }
}

/** The player covers the screen: a category Back left under it closes, over the section it covered before. */
internal fun AppScene.closeExploreCategoryUnderPlayer() {
    if (!shell.exploreCategoryClosesUnderPlayer) return
    shell.exploreCategoryClosesUnderPlayer = false
    shell.exploreCloseCategory = OneShotRequest(Unit)
    shell.section = sectionAfterPlayerExpandsOverCategory(shell.section, shell.categoryPlayerSection)
}
