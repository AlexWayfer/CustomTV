package name.alexwayfer.customtv.ui.settings

import androidx.annotation.StringRes
import java.util.Locale

/** Resource identities are shared with the controls; no translated text or saved setting enters the catalogue. */
internal data class SettingsSearchEntry(
    val page: SettingsPage,
    @param:StringRes val title: Int,
    @param:StringRes val hint: Int? = null,
    @param:StringRes val keywords: Int? = null,
    @param:StringRes val anchorTitle: Int = title,
    @param:StringRes val anchorHint: Int? = null,
) {
    val id: String get() = "${page.name}:$title:${hint ?: 0}"
}

internal data class SettingsSearchDocument(
    val entry: SettingsSearchEntry,
    val title: String,
    val hint: String,
    val path: String,
    val keywords: String,
)

internal data class SettingsSearchState(
    val query: String = "",
    val active: Boolean = false,
    val selectedId: String? = null,
)

internal fun openSettingsSearch(state: SettingsSearchState): SettingsSearchState =
    state.copy(active = true, selectedId = null)

internal fun selectSettingsSearchResult(state: SettingsSearchState, id: String): SettingsSearchState =
    state.copy(active = false, selectedId = id)

/** A selected control returns to results; the Material search window handles its own Back. */
internal fun settingsSearchBack(state: SettingsSearchState): SettingsSearchState? =
    if (state.selectedId != null) state.copy(active = true, selectedId = null) else null

private enum class SearchMatch { ExactTitle, Title, Other }

/** All words must match; ties keep the catalogue's order, which follows the settings pages. */
internal fun searchSettings(documents: List<SettingsSearchDocument>, query: String): List<SettingsSearchDocument> {
    val normalized = normalizeSettingsSearch(query).trim()
    if (normalized.isEmpty()) return emptyList()
    val words = normalized.split(Regex("\\s+"))
    return documents.mapNotNull { document ->
        val title = normalizeSettingsSearch(document.title)
        val text = normalizeSettingsSearch("${document.title} ${document.hint} ${document.path} ${document.keywords}")
        if (!words.all { it in text }) return@mapNotNull null
        val match = when {
            title == normalized -> SearchMatch.ExactTitle
            words.all { it in title } -> SearchMatch.Title
            else -> SearchMatch.Other
        }
        document to match
    }.sortedBy { it.second }.map { it.first }
}

private fun normalizeSettingsSearch(text: String): String = text.lowercase(Locale.ROOT).replace('ё', 'е')
