package name.alexwayfer.customtv.ui.settings

import name.alexwayfer.customtv.R

/** Locked pages remain discoverable at their existing Premium rows, never through a blank subpage. */
internal fun editionSettingsSearchEntries(accountName: String?): List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(SettingsPage.Main, R.string.settings_section_notifications, R.string.premium_locked),
    SettingsSearchEntry(SettingsPage.Main, R.string.settings_hidden_categories, R.string.premium_locked),
    SettingsSearchEntry(SettingsPage.Main, R.string.settings_section_chatter_portraits, R.string.premium_locked),
)
