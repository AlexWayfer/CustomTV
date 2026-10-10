package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLabel
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.data.channelSearchExtras
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchHint
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText

fun searchHistoryIsLive(profileLive: Boolean, followedLive: Boolean): Boolean {
    return profileLive || followedLive
}

/** The shape of the field on the empty Home, which the search opens out of. */
internal val ChannelSearchFieldShape: Shape = RoundedCornerShape(8.dp)

/** The shape of the search button, which the search opens out of and closes back into. */
internal val ChannelSearchButtonShape: Shape = CircleShape

/** Below Material's 56dp search field, at the 48dp minimum touch target. */
private val ChannelSearchHeight = 48.dp

// The field keeps its own height and padding inside and is cropped evenly, so its text stays centered.
private fun Modifier.channelSearchHeight() =
    height(ChannelSearchHeight).wrapContentHeight(unbounded = true)
private val ChannelSearchErrorColor = Color(0xFFEB0400)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun channelSearchInputColors(): TextFieldColors = SearchBarDefaults.inputFieldColors(
    focusedTextColor = TwitchText,
    unfocusedTextColor = TwitchText,
    cursorColor = TwitchPurple,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun channelSearchBarColors(): SearchBarColors = SearchBarDefaults.colors(
    containerColor = TwitchSurfaceAlt,
    dividerColor = TwitchDivider,
    inputFieldColors = channelSearchInputColors(),
)

/** The search button beside the title: the full-screen search opens out of it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelSearchButton(state: SearchBarState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = { scope.launch { state.animateToExpanded() } },
        modifier = modifier.onGloballyPositioned { state.collapsedCoords = it },
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.channel_search_open),
            tint = TwitchText,
        )
    }
}

/**
 * The field on the empty Home. It only opens the search: read-only, it takes focus without asking for the
 * keyboard, so the keyboard opens once, for the full-screen search's field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelSearchCollapsedField(state: SearchBarState, value: String, modifier: Modifier = Modifier) {
    val text = rememberTextFieldState(value)
    LaunchedEffect(value) {
        if (text.text.toString() != value) text.setTextAndPlaceCursorAtEnd(value)
    }
    SearchBar(
        state = state,
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = text,
                searchBarState = state,
                onSearch = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .channelSearchHeight(),
                readOnly = true,
                placeholder = { Text(stringResource(R.string.channel_hint), color = TwitchHint) },
                // The same icon as the search button; the field itself is the action, so it is not announced.
                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TwitchText) },
                colors = channelSearchInputColors(),
            )
        },
        modifier = modifier,
        shape = ChannelSearchFieldShape,
        colors = channelSearchBarColors(),
    )
}

/**
 * The full-screen channel search, opened from the button or the field that share [state] and closing back into
 * [collapsedShape]. It stays open while [checking] a typed or picked channel and shows the [error] when that
 * fails; Home closes it once the channel opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelSearchExpanded(
    state: SearchBarState,
    collapsedShape: Shape,
    value: String,
    onValueChange: (String) -> Unit,
    history: List<String>,
    profiles: Map<String, ChannelProfile>,
    followed: List<FollowedChannel>,
    searchHits: List<ChannelSearchHit>,
    liveLogins: Set<String>,
    error: String?,
    checking: Boolean,
    onSubmit: () -> Unit,
    onPick: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    // The keyboard belongs to the search's own window while it is open; the screen below ignores it.
    val open = state.currentValue == SearchBarValue.Expanded
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    DisposableEffect(open) {
        currentOnExpandedChange(open)
        onDispose { if (open) currentOnExpandedChange(false) }
    }
    val historyMatches = channelSearchSuggestions(history, profiles, value)
    val followedMatches = followedSearchSuggestions(followed, historyMatches, value)
    val extras = channelSearchExtras(historyMatches + followedMatches.map { it.login }, searchHits)
    val showChecking = rememberHeldWhileLoading(checking, loading = checking)
    // A state rather than a plain string, so text put in from outside, such as a picked suggestion, gets the
    // cursor at its end instead of keeping the cursor where the typing was.
    val text = rememberTextFieldState(value)
    LaunchedEffect(value) {
        if (text.text.toString() != value) text.setTextAndPlaceCursorAtEnd(value)
    }
    // Only typing goes up: the text put in from [value] itself must not search again or clear the error.
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(text) {
        snapshotFlow { text.text.toString() }.collect { typed ->
            if (typed != currentValue) currentOnValueChange(typed)
        }
    }

    // The search's window takes its status bar icons from the content color here: light, for the dark screen.
    CompositionLocalProvider(LocalContentColor provides TwitchText) {
        ExpandedFullScreenSearchBar(
            state = state,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = text,
                    searchBarState = state,
                    onSearch = { onSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { if (error != null) error(error) },
                    placeholder = { Text(stringResource(R.string.channel_hint), color = TwitchHint) },
                    trailingIcon = if (showChecking) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TwitchPurple,
                                trackColor = TwitchDivider,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        null
                    },
                    colors = channelSearchInputColors(),
                )
            },
            collapsedShape = collapsedShape,
            colors = channelSearchBarColors(),
        ) {
            if (error != null) {
                Text(
                    text = error,
                    color = ChannelSearchErrorColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                )
            }
            LazyColumn {
                items(historyMatches, key = { "history:$it" }) { login ->
                    ChannelSearchItem(
                        login = login,
                        profile = profiles[login],
                        liveLogins = liveLogins,
                        onPick = { onPick(login) },
                    )
                }
                items(followedMatches, key = { "followed:${it.id}" }) { channel ->
                    ChannelSearchItem(
                        login = channel.login,
                        profile = ChannelProfile(
                            login = channel.login,
                            displayName = channel.displayName,
                            avatarUrl = channel.avatarUrl,
                            collaboratorAvatarUrls = channel.collaboratorAvatarUrls,
                            isLive = channel.isLive,
                        ),
                        liveLogins = liveLogins,
                        onPick = { onPick(channel.login) },
                    )
                }
                items(extras, key = { "hit:${it.login}" }) { hit ->
                    ChannelSearchItem(
                        login = hit.login,
                        profile = ChannelProfile(
                            login = hit.login,
                            displayName = hit.displayName,
                            avatarUrl = hit.avatarUrl,
                            isLive = hit.isLive,
                        ),
                        liveLogins = liveLogins,
                        onPick = { onPick(hit.login) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelSearchItem(
    login: String,
    profile: ChannelProfile?,
    liveLogins: Set<String>,
    onPick: () -> Unit,
) {
    val label = displayNameLabel(profile?.displayName ?: login, login)
    val live = searchHistoryIsLive(
        profileLive = profile?.isLive == true,
        followedLive = login in liveLogins,
    )
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (live) {
                    val liveDescription = stringResource(R.string.search_history_live)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TwitchLive)
                            .semantics { contentDescription = liveDescription },
                    )
                }
            }
        },
        leadingContent = {
            ChannelAvatarWithCollaborators(
                channel = login,
                profile = profile,
                collaboratorAvatarUrls = if (live) profile?.collaboratorAvatarUrls.orEmpty() else emptyList(),
                ringColor = TwitchSurfaceAlt,
                size = 32.dp,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = TwitchText,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick),
    )
}
