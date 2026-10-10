package name.alexwayfer.customtv.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/**
 * Waits for the login in the browser. The page can close without telling the app, such as a plain browser tab, so
 * the page can be opened again or the login dropped from here. [pageReady] is false while the login link loads.
 * Back drops the login too: left to the system, it would close the app and end the login unseen.
 */
@Composable
internal fun AccountLoginScreen(
    pageReady: Boolean,
    onReopen: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.twitch_device_waiting),
            modifier = Modifier.padding(top = 16.dp),
            color = TwitchTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        AnimatedVisibility(
            visible = pageReady,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Button(onClick = onReopen, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.twitch_login_reopen))
            }
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = TwitchTextSecondary),
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}
