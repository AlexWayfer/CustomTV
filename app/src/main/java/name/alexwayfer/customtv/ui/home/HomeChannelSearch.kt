package name.alexwayfer.customtv.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.isValidChannelLogin
import name.alexwayfer.customtv.channel.normalizeChannelInput
import name.alexwayfer.customtv.data.ChannelLookup

/**
 * Home's full-screen channel search over [channelInput]. A typed or picked channel opens once Twitch knows it, with
 * the search closed first; otherwise the search stays open with the reason.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeChannelSearch(
    viewModel: HomeViewModel,
    state: SearchBarState,
    searchEntry: HomeSearchEntry,
    channelInput: MutableState<String>,
    signedIn: Boolean,
    onOpen: (login: String, live: Boolean) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val recentProfiles by viewModel.recentProfiles.collectAsStateWithLifecycle()
    val channelSearch by viewModel.channelSearch.collectAsStateWithLifecycle()
    val checkingChannel by viewModel.checkingChannel.collectAsStateWithLifecycle()
    val followedChannels by viewModel.follows.channels.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    val submit = homeChannelSubmit(viewModel, state, channelInput, onError = { error = it }, onOpen = onOpen)
    ChannelSearchExpanded(
        state = state,
        collapsedShape =
            if (searchEntry == HomeSearchEntry.Field) ChannelSearchFieldShape else ChannelSearchButtonShape,
        value = channelInput.value,
        onValueChange = {
            channelInput.value = it
            error = null
            viewModel.updateChannelSearch(it)
        },
        history = recents.map { it.login },
        profiles = recentProfiles,
        followed = if (signedIn) followedChannels else emptyList(),
        searchHits = channelSearch,
        liveLogins = followedChannels.mapNotNullTo(mutableSetOf()) { channel ->
            channel.login.lowercase().takeIf { channel.isLive }
        },
        error = error,
        checking = checkingChannel,
        onSubmit = { submit(channelInput.value) },
        onPick = { login ->
            channelInput.value = login
            submit(login)
        },
        onExpandedChange = { expanded ->
            onSearchOpenChange(expanded)
            if (!expanded) error = null
            if (refreshRecentsOnSearchOpen(signedIn, expanded, recents.isNotEmpty())) {
                viewModel.refreshRecents(showProgress = false, automatic = true)
            }
        },
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
