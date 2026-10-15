package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLabel
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.FollowedChannel
import name.alexwayfer.customtv.ui.theme.TwitchHint
import name.alexwayfer.customtv.ui.theme.TwitchLive
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
                colors = searchInputColors(),
            )
        },
        modifier = modifier,
        shape = ChannelSearchFieldShape,
        colors = searchBarColors(),
    )
}

/** The channels the search suggests for the [value] typed: the recents, the follows, then what Twitch found. */
@Composable
internal fun ChannelSearchSuggestions(
    value: String,
    history: List<String>,
    profiles: Map<String, ChannelProfile>,
    followed: List<FollowedChannel>,
    searchHits: List<ChannelSearchHit>,
    liveLogins: Set<String>,
    onPick: (String) -> Unit,
) {
    val matches = channelSearchMatches(history, profiles, followed, searchHits, value)
    LazyColumn {
        items(matches.history, key = { "history:$it" }) { login ->
            ChannelSearchItem(
                login = login,
                profile = profiles[login],
                liveLogins = liveLogins,
                onPick = { onPick(login) },
            )
        }
        items(matches.followed, key = { "followed:${it.id}" }) { channel ->
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
        items(matches.found, key = { "hit:${it.login}" }) { hit ->
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
