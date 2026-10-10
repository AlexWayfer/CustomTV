package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/** Before an update installs: what to tap when Play Protect blocks an app from outside Google Play. */
@Composable
internal fun PlayProtectHintDialog(onInstall: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_update_play_protect_title)) },
        text = {
            Text(
                text = stringResource(R.string.app_update_play_protect_text),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text(stringResource(R.string.app_update_play_protect_install))
            }
        },
    )
}
