package name.alexwayfer.customtv.ui.settings

import android.os.Build
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.links.appLinksNeedSetup

/** Only controls available on this device and in this edition are offered. */
internal fun settingsSearchEntries(accountName: String?, sdkInt: Int): List<SettingsSearchEntry> = buildList {
    addAll(chatSearchEntries())
    addAll(mentionSearchEntries())
    add(SettingsSearchEntry(SettingsPage.Playback, R.string.settings_section_playback,
        R.string.settings_page_playback_hint))
    add(SettingsSearchEntry(SettingsPage.Playback, R.string.background_sound_only, R.string.background_sound_only_hint,
        R.string.settings_search_sound_keywords))
    add(SettingsSearchEntry(SettingsPage.Playback, R.string.smooth_sound, R.string.smooth_sound_hint,
        R.string.settings_search_sound_keywords))
    add(SettingsSearchEntry(SettingsPage.ChatterLabels, R.string.chatter_labels, R.string.chatter_labels_hint))
    add(SettingsSearchEntry(SettingsPage.ChatterLabels, R.string.chatter_labels_token,
        R.string.chatter_labels_create_token))
    add(SettingsSearchEntry(SettingsPage.ChatterLabels, R.string.chatter_labels_gist_id,
        R.string.chatter_labels_gist_id_hint))
    addAll(editionSettingsSearchEntries(accountName))
    add(SettingsSearchEntry(SettingsPage.Main, R.string.settings_section_updates))
    add(SettingsSearchEntry(SettingsPage.Main, R.string.app_update_check,
        anchorTitle = R.string.settings_section_updates))
    add(SettingsSearchEntry(SettingsPage.Main, R.string.settings_section_premium))
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        add(SettingsSearchEntry(SettingsPage.Main, R.string.settings_section_language))
    }
    if (appLinksNeedSetup(sdkInt)) {
        add(SettingsSearchEntry(SettingsPage.Main, R.string.app_links_setting,
            anchorTitle = R.string.settings_section_app_links))
    }
    add(SettingsSearchEntry(SettingsPage.Gestures, R.string.gestures, R.string.gestures_hint))
    add(SettingsSearchEntry(SettingsPage.Licenses, R.string.settings_licenses, R.string.settings_licenses_hint))
    add(SettingsSearchEntry(SettingsPage.Main, R.string.diagnostics_channel))
    add(SettingsSearchEntry(SettingsPage.Main, R.string.diagnostics_send, anchorTitle = R.string.diagnostics_channel))
}

private fun chatSearchEntries(): List<SettingsSearchEntry> = buildList {
    add(SettingsSearchEntry(SettingsPage.Chat, R.string.settings_page_chat, R.string.settings_page_chat_hint))
    listOf(
        R.string.third_party_emotes to R.string.seven_tv_emotes_hint,
        R.string.third_party_badges to R.string.seven_tv_badges_hint,
        R.string.seven_tv_paints to R.string.seven_tv_paints_hint,
        R.string.third_party_emotes to R.string.ffz_emotes_hint,
        R.string.third_party_badges to R.string.ffz_badges_hint,
        R.string.third_party_emotes to R.string.bttv_emotes_hint,
        R.string.third_party_badges to R.string.bttv_badges_hint,
        R.string.third_party_badges to R.string.custom_tv_badges_hint,
        R.string.own_custom_tv_badge to R.string.own_custom_tv_badge_hint,
    ).forEach { (title, hint) ->
        val keywords = when (title) {
            R.string.third_party_emotes -> R.string.settings_search_emotes_keywords
            R.string.seven_tv_paints -> R.string.settings_search_paints_keywords
            else -> R.string.settings_search_badges_keywords
        }
        add(SettingsSearchEntry(SettingsPage.Chat, title, hint, keywords, anchorHint = hint))
    }
    listOf(
        R.string.me_messages to R.string.me_messages_hint,
        R.string.highlight_first_messages to R.string.highlight_first_messages_hint,
        R.string.emote_completion_without_colon to R.string.emote_completion_without_colon_hint,
        R.string.keep_keyboard_after_send to R.string.keep_keyboard_after_send_hint,
        R.string.scroll_to_latest_after_send to R.string.scroll_to_latest_after_send_hint,
        R.string.sent_message_history to R.string.sent_message_history_hint,
    ).forEach { (title, hint) -> add(SettingsSearchEntry(SettingsPage.Chat, title, hint)) }
}

private fun mentionSearchEntries(): List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(SettingsPage.Mentions, R.string.settings_page_mentions, R.string.settings_page_mentions_hint),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.highlight_mentions, R.string.highlight_mentions_hint),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.mention_vibration, R.string.mention_vibration_hint),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.mention_vibration_length,
        anchorTitle = R.string.mention_vibration),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.mention_vibration_strength,
        anchorTitle = R.string.mention_vibration),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.mention_sound, R.string.mention_sound_hint,
        R.string.settings_search_sound_keywords),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.mention_sound_choice, R.string.mention_sound_choice_hint,
        R.string.settings_search_sound_keywords, anchorTitle = R.string.mention_sound),
    SettingsSearchEntry(SettingsPage.Mentions, R.string.load_recent_chat_on_open,
        R.string.load_recent_chat_on_open_hint),
)
