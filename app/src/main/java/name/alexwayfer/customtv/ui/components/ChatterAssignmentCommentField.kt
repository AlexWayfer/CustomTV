package name.alexwayfer.customtv.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.singleLineChatterComment

/** A comment on one assignment, with its subject named for accessibility and no line breaks in the Gist. */
@Composable
internal fun ChatterAssignmentCommentField(
    value: String,
    subject: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.chatter_labels_comment_for, subject)
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(singleLineChatterComment(it)) },
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.chatter_labels_comment)) },
        modifier = modifier.semantics { contentDescription = description },
    )
}
