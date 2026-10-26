package name.alexwayfer.customtv.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.components.ChannelPreview

/**
 * Home's title row with its search button, for another section at the top of the app: the same search with the
 * recents opens from it, in the mode it [opensIn]. Leaving the section closes the search's hold on the navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelSearchTitle(
    viewModel: HomeViewModel,
    signedIn: Boolean,
    opensIn: SearchMode,
    onOpen: (channel: ChannelPreview, live: Boolean) -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) {
    val searchState = rememberSearchBarState()
    val channelInput = rememberSaveable { mutableStateOf("") }
    val searchOpenChange by rememberUpdatedState(onSearchOpenChange)
    DisposableEffect(Unit) {
        onDispose { searchOpenChange(false) }
    }
    HomeTitleRow(searchState, searchShown = true)
    HomeChannelSearch(
        viewModel = viewModel,
        state = searchState,
        searchEntry = HomeSearchEntry.Button,
        channelInput = channelInput,
        signedIn = signedIn,
        opensIn = opensIn,
        // The section with the title is Explore, which a category opens in.
        categoryShowsUnderSearch = true,
        onOpen = onOpen,
        onOpenCategory = onOpenCategory,
        onSearchOpenChange = onSearchOpenChange,
    )
}
