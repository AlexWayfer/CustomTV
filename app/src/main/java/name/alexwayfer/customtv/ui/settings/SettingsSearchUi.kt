package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

/** Localize on composition so a language change also updates an open search. */
@Composable
internal fun settingsSearchDocuments(entries: List<SettingsSearchEntry>): List<SettingsSearchDocument> =
    entries.map { entry ->
        SettingsSearchDocument(
            entry = entry,
            title = stringResource(entry.title),
            hint = entry.hint?.let { stringResource(it) }.orEmpty(),
            path = if (entry.anchorTitle != entry.title) {
                stringResource(entry.page.titleRes()) + " › " + stringResource(entry.anchorTitle)
            } else {
                stringResource(entry.page.titleRes())
            },
            keywords = entry.keywords?.let { stringResource(it) }.orEmpty(),
        )
    }

@Composable
internal fun SettingsSearchResults(
    documents: List<SettingsSearchDocument>,
    query: String,
    scroll: LazyListState,
    onSelect: (SettingsSearchEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = remember(documents, query) { searchSettings(documents, query) }
    LazyColumn(modifier = modifier.fillMaxSize(), state = scroll) {
        if (results.isEmpty()) {
            item {
                val message = if (query.isBlank()) R.string.settings_search_hint else R.string.settings_search_empty
                Text(
                    stringResource(message),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(results, key = { it.entry.id }) { result ->
            ListItem(
                headlineContent = { Text(result.title) },
                supportingContent = {
                    Column {
                        if (result.path != result.title) {
                            Text(result.path, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (result.hint.isNotBlank()) Text(result.hint)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(result.entry) },
            )
        }
    }
}
