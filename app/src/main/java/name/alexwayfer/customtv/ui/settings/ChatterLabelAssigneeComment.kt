package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.components.ChatterAssignmentCommentField

/** The extension's comment toggle beside a chatter, and an inline editor that keeps its draft after a failed save. */
@Composable
internal fun ChatterLabelAssigneeComment(
    assignee: ChatterLabelAssignee,
    enabled: Boolean,
    onSave: suspend (String) -> UiText?,
    content: @Composable RowScope.() -> Unit,
) {
    var expanded by rememberSaveable(assignee.comment) { mutableStateOf(assignee.comment.isNotEmpty()) }
    var edited by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val comment = edited ?: assignee.comment
    val scope = rememberCoroutineScope()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
            IconToggleButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                enabled = enabled && !saving,
            ) {
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.ic_chat_bubble else R.drawable.ic_chat_bubble_outline,
                    ),
                    contentDescription = stringResource(R.string.chatter_labels_comment_toggle, assignee.shownName),
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            AssigneeCommentEditor(
                assignee = assignee,
                comment = comment,
                enabled = enabled && !saving,
                error = error,
                onChange = { edited = it; error = null },
                onSave = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            error = onSave(comment)
                            if (error == null) {
                                edited = null
                                expanded = comment.isNotEmpty()
                            }
                        } finally {
                            saving = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AssigneeCommentEditor(
    assignee: ChatterLabelAssignee,
    comment: String,
    enabled: Boolean,
    error: UiText?,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatterAssignmentCommentField(
                value = comment,
                subject = assignee.shownName,
                enabled = enabled,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
            )
            IconButton(enabled = enabled && comment != assignee.comment, onClick = onSave) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.chatter_labels_save),
                )
            }
        }
        error?.let {
            Text(
                text = it.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
