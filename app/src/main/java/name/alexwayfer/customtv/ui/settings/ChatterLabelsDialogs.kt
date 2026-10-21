package name.alexwayfer.customtv.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.TextButton

/** Asks before a saved label goes, with how many chatters lose it and the way to keep them instead. */
@Composable
internal fun ChatterLabelDeleteDialog(
    name: String,
    chatters: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (name.isBlank()) {
                    stringResource(R.string.chatter_labels_delete_title_unnamed)
                } else {
                    stringResource(R.string.chatter_labels_delete_title, name.trim())
                },
            )
        },
        text = if (chatters > 0) {
            {
                Column {
                    Text(pluralStringResource(R.plurals.chatter_labels_delete_chatters, chatters, chatters))
                    Text(
                        text = stringResource(R.string.chatter_labels_delete_keep_hint),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            null
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.chatter_labels_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** Asks before leaving the page with label changes that are not saved. */
@Composable
internal fun ChatterLabelsDiscardDialog(onDiscard: () -> Unit, onStay: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.chatter_labels_discard_title)) },
        text = { Text(stringResource(R.string.chatter_labels_discard_body)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.chatter_labels_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onStay) {
                Text(stringResource(R.string.chatter_labels_stay))
            }
        },
    )
}
