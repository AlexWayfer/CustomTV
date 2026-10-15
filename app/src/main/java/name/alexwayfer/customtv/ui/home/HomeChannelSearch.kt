package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.isValidChannelLogin
import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.data.ChannelLookup
import name.alexwayfer.customtv.data.TwitchCategory
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/**
 * Home's full-screen search over [channelInput], of channels or, where they are offered, of categories, in the mode
 * it [opensIn] until another is picked. A typed or picked channel opens once Twitch knows it, with the search closed
 * first; otherwise the search stays open with the reason. A picked category goes to [onOpenCategory], under the
 * search when it [categoryShowsUnderSearch].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeChannelSearch(
    viewModel: HomeViewModel,
    state: SearchBarState,
    searchEntry: HomeSearchEntry,
    channelInput: MutableState<String>,
    signedIn: Boolean,
    opensIn: SearchMode,
    categoryShowsUnderSearch: Boolean,
    onOpen: (login: String, live: Boolean) -> Unit,
    onOpenCategory: (TwitchCategory) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val checkingChannel by viewModel.checkingChannel.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    var pickedMode by rememberSaveable { mutableStateOf<SearchMode?>(null) }
    val categoriesOffered = categorySearchOffered()
    val mode = shownSearchMode(pickedMode, opensIn, categoriesOffered)
    var categorySubmit by remember { mutableStateOf<OneShotRequest<Unit>?>(null) }
    val scope = rememberCoroutineScope()
    val submit = homeChannelSubmit(viewModel, state, channelInput, onError = { error = it }, onOpen = onOpen)
    val channels = mode == SearchMode.Channels
    FullScreenSearch(
        state = state,
        collapsedShape =
            if (searchEntry == HomeSearchEntry.Field) ChannelSearchFieldShape else ChannelSearchButtonShape,
        value = channelInput.value,
        onValueChange = {
            channelInput.value = it
            error = null
            if (channels) viewModel.updateChannelSearch(it)
        },
        placeholder = if (channels) R.string.channel_hint else categorySearchHint(),
        onSubmit = {
            if (channels) submit(channelInput.value) else categorySubmit = OneShotRequest(Unit)
        },
        onExpandedChange = { expanded ->
            onSearchOpenChange(expanded)
            if (!expanded) error = null
            if (refreshRecentsOnSearchOpen(signedIn, expanded, recents.isNotEmpty())) {
                viewModel.refreshRecents(showProgress = false, automatic = true)
            }
        },
        error = error.takeIf { channels },
        checking = checkingChannel && channels,
    ) {
        // Gone with the search's window, once it has closed, so the next search opens in its own mode again.
        DisposableEffect(Unit) {
            onDispose { pickedMode = null }
        }
        HomeSearchPages(
            viewModel = viewModel,
            value = channelInput.value,
            signedIn = signedIn,
            mode = if (categoriesOffered) mode else null,
            categorySubmit = categorySubmit,
            onMode = { pickedMode = it },
            onPickChannel = { login ->
                channelInput.value = login
                submit(login)
            },
            onCategory = { category ->
                scope.launch {
                    openPickedCategory(state, categoryShowsUnderSearch) { onOpenCategory(category) }
                    // Cleared as for an opened channel, the category being first in the history now; only once the
                    // search has closed, so its list does not turn to the history while it closes.
                    channelInput.value = ""
                    viewModel.updateChannelSearch("")
                }
            },
        )
    }
}

/**
 * The search's suggestions: the channels alone without a [mode], otherwise a page for each mode, starting on [mode].
 */
@Composable
private fun ColumnScope.HomeSearchPages(
    viewModel: HomeViewModel,
    value: String,
    signedIn: Boolean,
    mode: SearchMode?,
    categorySubmit: OneShotRequest<Unit>?,
    onMode: (SearchMode) -> Unit,
    onPickChannel: (String) -> Unit,
    onCategory: (TwitchCategory) -> Unit,
) {
    val channels = @Composable { HomeChannelSuggestions(viewModel, value, signedIn, onPickChannel) }
    if (mode == null) {
        channels()
        return
    }
    SearchModePager(
        mode = mode,
        onMode = { shown ->
            // The channels were not asked while the categories showed.
            if (shown == SearchMode.Channels && mode != shown) viewModel.updateChannelSearch(value)
            onMode(shown)
        },
        channels = channels,
        categories = { CategorySearchSuggestions(value, categorySubmit, onCategory) },
    )
}

/** The channels Home's search suggests for the [value] typed, a pick going to [onPick]. */
@Composable
private fun HomeChannelSuggestions(
    viewModel: HomeViewModel,
    value: String,
    signedIn: Boolean,
    onPick: (String) -> Unit,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val recentProfiles by viewModel.recentProfiles.collectAsStateWithLifecycle()
    val channelSearch by viewModel.channelSearch.collectAsStateWithLifecycle()
    val followedChannels by viewModel.follows.channels.collectAsStateWithLifecycle()
    ChannelSearchSuggestions(
        value = value,
        history = recents.map { it.login },
        profiles = recentProfiles,
        followed = if (signedIn) followedChannels else emptyList(),
        searchHits = channelSearch,
        liveLogins = followedChannels.mapNotNullTo(mutableSetOf()) { channel ->
            channel.login.lowercase().takeIf { channel.isLive }
        },
        onPick = onPick,
    )
}

/**
 * Opens a typed channel once Twitch knows it, with the search closed first, or reports through [onError] why it
 * cannot; nothing while a channel is still being checked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun homeChannelSubmit(
    viewModel: HomeViewModel,
    searchState: SearchBarState,
    channelInput: MutableState<String>,
    onError: (String?) -> Unit,
    onOpen: (login: String, live: Boolean) -> Unit,
): (String) -> Unit {
    val checkingChannel by viewModel.checkingChannel.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val invalidMessage = stringResource(R.string.invalid_channel)
    val missingMessage = stringResource(R.string.channel_not_found)
    val lookupFailedMessage = stringResource(R.string.channel_lookup_failed)
    return submit@{ raw ->
        if (checkingChannel) return@submit
        val login = normalizeChannelInput(raw)
        if (!isValidChannelLogin(login)) {
            onError(invalidMessage)
            return@submit
        }
        onError(null)
        focusManager.clearFocus()
        viewModel.openTypedChannel(login) { result ->
            when (result) {
                is ChannelLookup.Found -> {
                    channelInput.value = channelInputAfterLookup(channelInput.value, result)
                    viewModel.updateChannelSearch(channelInput.value)
                    // Closed before Home goes: an animation would be cancelled with it, and the open search
                    // would be saved and come back over the next screen that shows Home again.
                    scope.launch {
                        searchState.snapTo(0f)
                        onOpen(result.profile.login, result.profile.isLive)
                    }
                }
                ChannelLookup.NotFound -> onError(missingMessage)
                ChannelLookup.Unavailable -> onError(lookupFailedMessage)
            }
        }
    }
}
