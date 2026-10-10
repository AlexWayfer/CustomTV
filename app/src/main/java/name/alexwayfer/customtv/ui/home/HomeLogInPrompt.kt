package name.alexwayfer.customtv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.HomeLogInPromptStore
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchText

/** Whether Home offers to log in to Twitch, and the action that puts the offer away until the next log out. */
internal class HomeLogInPromptState(val shown: Boolean, val dismiss: () -> Unit)

@Composable
internal fun rememberHomeLogInPrompt(signedIn: Boolean): HomeLogInPromptState {
    val context = LocalContext.current
    val store = remember(context) { HomeLogInPromptStore(context) }
    val dismissed by store.dismissed.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    return HomeLogInPromptState(
        shown = homeLogInPromptShown(signedIn, dismissed),
        dismiss = { scope.launch { store.markDismissed() } },
    )
}

/**
 * The offer shows logged out until Not now puts it away. Before the saved answer is read ([dismissed] null) it
 * stays hidden, so it never shows for a moment and then goes.
 */
internal fun homeLogInPromptShown(signedIn: Boolean, dismissed: Boolean?): Boolean =
    !signedIn && dismissed == false

/** A small card: why to log in, with Not now and Log in below the text. */
@Composable
internal fun HomeLogInPromptCard(onLogIn: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = TwitchText,
        ),
    ) {
        Text(
            text = stringResource(R.string.home_log_in_for_follows),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TwitchText),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.home_log_in_prompt_dismiss))
            }
            Button(
                onClick = onLogIn,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TwitchPurple, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.log_in))
            }
        }
    }
}
