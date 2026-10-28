package name.alexwayfer.customtv.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.OutlinedButton
import name.alexwayfer.customtv.ui.components.TextButton

/** Before a report goes out: what personal data it holds, and the advice to send it privately. */
@Composable
internal fun DiagnosticSendDialog(
    items: List<DiagnosticPrivateItem>,
    onEmail: () -> Unit,
    onOther: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diagnostics_send_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.diagnostics_privacy_advice))
                Text(stringResource(R.string.diagnostics_privacy_data))
                Column {
                    items.forEach { item ->
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text(text = "•", modifier = Modifier.clearAndSetSemantics {})
                            Text(text = privateItemText(item), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        // One action a line at the end: the advised one filled on top, Cancel at the bottom.
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onEmail) { Text(stringResource(R.string.diagnostics_send_email)) }
                OutlinedButton(onClick = onOther) { Text(stringResource(R.string.diagnostics_send_other)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun privateItemText(item: DiagnosticPrivateItem): String = when (item) {
    is DiagnosticPrivateItem.TwitchAccount -> stringResource(R.string.diagnostics_privacy_twitch, item.name)
    is DiagnosticPrivateItem.TelegramAccount -> stringResource(R.string.diagnostics_privacy_telegram, item.name)
    DiagnosticPrivateItem.Viewing -> stringResource(R.string.diagnostics_privacy_viewing)
    DiagnosticPrivateItem.Device -> stringResource(R.string.diagnostics_privacy_device)
}
