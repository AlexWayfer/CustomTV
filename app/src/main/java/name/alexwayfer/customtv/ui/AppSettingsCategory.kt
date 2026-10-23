package name.alexwayfer.customtv.ui

import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** Opens [category] in Explore from the hidden categories in the settings, which Back from it returns to. */
internal fun AppScene.openExploreCategoryFromSettings(category: TwitchCategory) {
    shell.exploreOpenCategory = OneShotRequest(ExploreCategoryOpen(category, from = ExploreCategoryFrom.Settings))
    shell.section = AppSection.Explore
}

/** The hidden categories in the settings, which Back from a category they opened returns to. */
internal fun AppScene.showHiddenCategories() {
    shell.settingsScrollTarget = SettingsScrollTarget.HiddenCategories
    shell.section = AppSection.Settings
}
