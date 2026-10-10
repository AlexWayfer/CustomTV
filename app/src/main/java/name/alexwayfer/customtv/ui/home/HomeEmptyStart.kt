package name.alexwayfer.customtv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R

/**
 * Home logged out with no recents: the channel search on top, where searching is the only thing to do, and the
 * offer to log in for the followed channels right below it, until it is put away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeEmptyStart(
    searchState: SearchBarState,
    channelInput: String,
    logInPrompt: HomeLogInPromptState,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        FollowedSectionHeader(text = stringResource(R.string.channel_search_open))
        Spacer(Modifier.height(8.dp))
        ChannelSearchCollapsedField(
            state = searchState,
            value = channelInput,
            modifier = Modifier.fillMaxWidth(),
        )
        // Right under the search, where the list shows it under its title.
        AnimatedVisibility(
            visible = logInPrompt.shown,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            HomeLogInPromptCard(
                onLogIn = onLogIn,
                onDismiss = logInPrompt.dismiss,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
