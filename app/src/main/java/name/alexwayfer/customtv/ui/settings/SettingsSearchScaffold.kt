package name.alexwayfer.customtv.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.home.ChannelSearchButtonShape
import name.alexwayfer.customtv.ui.home.FullScreenSearch
import name.alexwayfer.customtv.ui.watch.OneShotRequest

private val SearchStateSaver = listSaver<SettingsSearchState, Any>(
    save = { listOf(it.query, it.active, it.selectedId.orEmpty()) },
    restore = { SettingsSearchState(it[0] as String, it[1] as Boolean, (it[2] as String).ifEmpty { null }) },
)

/** The settings keep their original surface and transitions underneath the shared full-screen search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSearchScaffold(
    page: SettingsPage,
    accountName: String?,
    leaveGuard: SettingsLeaveGuard,
    onBack: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    reselected: OneShotRequest<Unit>?,
    scrollTo: SettingsScrollTarget?,
    onReselect: () -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var search by rememberSaveable(stateSaver = SearchStateSaver) { mutableStateOf(SettingsSearchState()) }
    var origin by rememberSaveable { mutableStateOf(page) }
    var needsScroll by rememberSaveable { mutableStateOf(false) }
    var pendingResultOpen by rememberSaveable { mutableStateOf(false) }
    val searchBar = rememberSearchBarState()
    val entries = remember(accountName) { settingsSearchEntries(accountName, Build.VERSION.SDK_INT) }
    val documents = settingsSearchDocuments(entries)
    val selected = entries.find { it.id == search.selectedId }
    val scrollRequest = remember(selected?.id, needsScroll) { if (needsScroll) OneShotRequest(Unit) else null }
    val resultScroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    SettingsSearchNavigationEffects(reselected, scrollTo, leaveGuard, onReselect, onReset = {
        search = search.copy(active = false, selectedId = null)
        pendingResultOpen = false
        needsScroll = false
        scope.launch { searchBar.animateToCollapsed() }
    })
    SettingsSearchSelection(searchBar, pendingResultOpen) {
        selected?.let { onOpenPage(it.page) }
        needsScroll = true
        pendingResultOpen = false
    }
    val back = settingsSearchBackAction(search, leaveGuard, onBack) { next ->
        search = next
        needsScroll = false
        scope.launch { searchBar.animateToExpanded() }
    }
    BackHandler(enabled = search.selectedId != null && !search.active, onBack = back)
    val focus = settingsSearchFocusFor(selected?.takeIf { it.page == page }, scrollRequest) { needsScroll = false }
    SettingsSearchSurface(page, searchBar, focus, back, onSearch = {
        leaveGuard.leave {
            origin = page
            search = openSettingsSearch(search)
            needsScroll = false
            scope.launch { searchBar.animateToExpanded() }
        }
    }, content = content)
    SettingsSearchWindow(searchBar, search, documents, resultScroll,
        onQueryChange = {
            search = search.copy(query = it)
            scope.launch { resultScroll.scrollToItem(0) }
        },
        onSelect = { entry ->
            if (search.active) {
                search = selectSettingsSearchResult(search, entry.id)
                pendingResultOpen = true
            }
        },
        onClosed = {
            if (search.active) {
                search = search.copy(active = false)
                onOpenPage(origin)
            }
        },
        onSearchOpenChange = onSearchOpenChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSearchSurface(
    page: SettingsPage,
    searchBar: SearchBarState,
    focus: SettingsSearchFocus?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(LocalSettingsSearchFocus provides focus) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                SettingsTopBar(page, onBack, searchBar, onSearch,
                    showBack = page != SettingsPage.Main || focus != null)
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSearchWindow(
    searchBar: SearchBarState,
    search: SettingsSearchState,
    documents: List<SettingsSearchDocument>,
    scroll: LazyListState,
    onQueryChange: (String) -> Unit,
    onSelect: (SettingsSearchEntry) -> Unit,
    onClosed: () -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) {
    FullScreenSearch(
        state = searchBar,
        collapsedShape = ChannelSearchButtonShape,
        value = search.query,
        onValueChange = onQueryChange,
        placeholder = R.string.settings_search,
        onSubmit = null,
        onClear = { onQueryChange("") },
        onExpandedChange = { expanded ->
            onSearchOpenChange(expanded)
            if (!expanded) onClosed()
        },
    ) {
        SettingsSearchResults(documents, search.query, scroll, onSelect)
    }
}

/** A pending selection survives recreation; it opens once the search has finished closing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSearchSelection(
    searchBar: SearchBarState,
    pendingResultOpen: Boolean,
    onPicked: () -> Unit,
) {
    LaunchedEffect(pendingResultOpen) {
        if (!pendingResultOpen) return@LaunchedEffect
        searchBar.animateToCollapsed()
        onPicked()
    }
}

private fun settingsSearchBackAction(
    search: SettingsSearchState,
    leaveGuard: SettingsLeaveGuard,
    onBack: () -> Unit,
    onSearchBack: (SettingsSearchState) -> Unit,
): () -> Unit = {
    val next = settingsSearchBack(search)
    if (next == null) onBack() else leaveGuard.leave { onSearchBack(next) }
}

@Composable
private fun SettingsSearchNavigationEffects(
    reselected: OneShotRequest<Unit>?,
    scrollTo: SettingsScrollTarget?,
    leaveGuard: SettingsLeaveGuard,
    onReselect: () -> Unit,
    onReset: () -> Unit,
) {
    LaunchedEffect(reselected) {
        if (reselected?.take() == null) return@LaunchedEffect
        leaveGuard.leave {
            onReset()
            onReselect()
        }
    }
    LaunchedEffect(scrollTo) { if (scrollTo != null) onReset() }
}
