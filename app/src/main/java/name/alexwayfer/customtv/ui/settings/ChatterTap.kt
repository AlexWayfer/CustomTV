package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.components.ChannelPreview

/** What a tap on a chatter in a label's list does. */
internal sealed interface ChatterTap {
    /** Opens the profile of [channel]. */
    data class Open(val channel: ChannelPreview) : ChatterTap

    /** Tells that Twitch has banned the account. */
    data object Banned : ChatterTap

    /** Tells that Twitch does not know the account. */
    data object NotFound : ChatterTap

    /** Tells that the chatter's login is still loading. */
    data object Loading : ChatterTap

    /** Tells that the chatter's login could not be loaded. */
    data object LoadFailed : ChatterTap
}

/**
 * A chatter Twitch has [banned] tells so, as their profile would show nothing. Any other with a login opens their
 * profile. One without, as of an assignment from before the extension kept logins, tells why nothing opens: Twitch
 * does not know them ([notFound]), their name is still loading while [namesLoading], or the load failed.
 */
internal fun chatterTap(
    assignee: ChatterLabelAssignee,
    namesLoading: Boolean,
    banned: Set<String>,
    notFound: Set<String>,
): ChatterTap = when {
    assignee.userId in banned -> ChatterTap.Banned
    assignee.login.isNotBlank() -> ChatterTap.Open(ChannelPreview(assignee.login, assignee.displayName))
    assignee.userId in notFound -> ChatterTap.NotFound
    namesLoading -> ChatterTap.Loading
    else -> ChatterTap.LoadFailed
}

/** Opens the tapped chatter's profile with [onOpenProfile], or tells with [onNotice] why it cannot. */
@Composable
internal fun rememberChatterTap(
    state: ChatterLabelsEditorState,
    onOpenProfile: (ChannelPreview) -> Unit,
    onNotice: (String) -> Unit,
): (ChatterLabelAssignee) -> Unit {
    val banned = stringResource(R.string.chatter_labels_chatter_banned)
    val notFound = stringResource(R.string.chatter_labels_chatter_not_found)
    val loading = stringResource(R.string.chatter_labels_chatter_loading)
    val loadFailed = stringResource(R.string.chatter_labels_chatter_load_failed)
    val open by rememberUpdatedState(onOpenProfile)
    val notice by rememberUpdatedState(onNotice)
    return { assignee ->
        when (val tap = chatterTap(assignee, state.namesLoading, state.banned, state.notFound)) {
            is ChatterTap.Open -> open(tap.channel)
            ChatterTap.Banned -> notice(banned)
            ChatterTap.NotFound -> notice(notFound)
            ChatterTap.Loading -> notice(loading)
            ChatterTap.LoadFailed -> notice(loadFailed)
        }
    }
}
