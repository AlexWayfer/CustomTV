package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.ui.components.ChatterAssignmentCommentField

/** The enabled labels in extension order, with each saved date and any comment draft. */
@Composable
internal fun ChatterLabelAssignments(
    labels: List<ChatterLabelIcon>,
    assigned: List<ChatterLabelIcon>,
    checked: Set<String>,
    editedComments: Map<String, String>,
    enabled: Boolean,
    onCheckedChange: (String, Boolean) -> Unit,
    onCommentChange: (String, String) -> Unit,
) {
    labels.forEach { label ->
        key(label.id) {
            val saved = assigned.find { it.id == label.id } ?: label
            ChatterLabelAssignmentRow(
                label = saved,
                checked = label.id in checked,
                enabled = enabled,
                comment = editedComments[label.id] ?: saved.comment,
                onCheckedChange = { onCheckedChange(label.id, it) },
                onCommentChange = { onCommentChange(label.id, it) },
            )
        }
    }
}

/** The checkbox toggles the assignment; its comment remains editable only while checked, as in the extension. */
@Composable
internal fun ChatterLabelAssignmentRow(
    label: ChatterLabelIcon,
    checked: Boolean,
    enabled: Boolean,
    comment: String,
    onCheckedChange: (Boolean) -> Unit,
    onCommentChange: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.padding(12.dp),
            )
            ChatterLabelRow(label = label, modifier = Modifier.padding(vertical = 6.dp))
        }
        AnimatedVisibility(visible = checked) {
            ChatterAssignmentCommentField(
                value = comment,
                subject = label.name,
                enabled = enabled && checked,
                onValueChange = onCommentChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
        }
    }
}
