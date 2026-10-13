package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelProblem
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.data.decodeChatterLabelIcon
import sh.calvin.reorderable.ReorderableListItemScope

/**
 * One label in the settings: its icon, name, and whether chat shows it, with a handle to drag it and a way to
 * delete it. [chatters] is how many chatters have it in the Gist.
 */
@Composable
internal fun ReorderableListItemScope.ChatterLabelCard(
    label: EditableChatterLabel,
    chatters: Int,
    problems: Set<ChatterLabelProblem>,
    dragging: Boolean,
    enabled: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onChooseIcon: () -> Unit,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val elevation by animateDpAsState(if (dragging) 6.dp else 0.dp, label = "labelElevation")
    val moveUp = stringResource(R.string.chatter_labels_move_up)
    val moveDown = stringResource(R.string.chatter_labels_move_down)
    OutlinedCard(
        // TalkBack cannot drag, so it moves the label one place with an action.
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOfNotNull(
                    onMoveUp?.let { move -> CustomAccessibilityAction(moveUp) { move(); true } },
                    onMoveDown?.let { move -> CustomAccessibilityAction(moveDown) { move(); true } },
                )
            },
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = elevation),
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    modifier = Modifier
                        .draggableHandle(
                            enabled = enabled,
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragStopped = { haptic.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                        )
                        .clearAndSetSemantics {}
                        .padding(top = 8.dp)
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LabelIconButton(
                    icon = label.icon,
                    missing = ChatterLabelProblem.IconMissing in problems,
                    enabled = enabled,
                    onClick = onChooseIcon,
                )
                LabelNameField(label.name, problems, enabled, onNameChange)
                IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.chatter_labels_delete_label),
                    )
                }
            }
            LabelShownInChat(label.enabled, chatters, enabled, onEnabledChange)
        }
    }
}

/** Whether chat shows the label, with how many chatters have it. */
@Composable
private fun LabelShownInChat(shown: Boolean, chatters: Int, enabled: Boolean, onShownChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chatter_labels_show_in_chat)) },
        supportingContent = if (chatters > 0) {
            { Text(pluralStringResource(R.plurals.chatter_labels_chatters, chatters, chatters)) }
        } else {
            null
        },
        trailingContent = { Switch(checked = shown, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = shown,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onShownChange,
        ),
    )
}

/** The label's name, with the first of its [problems] under it. */
@Composable
private fun RowScope.LabelNameField(
    name: String,
    problems: Set<ChatterLabelProblem>,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
) {
    val problemText = when {
        ChatterLabelProblem.NameMissing in problems -> R.string.chatter_labels_error_name_missing
        ChatterLabelProblem.NameTaken in problems -> R.string.chatter_labels_error_name_taken
        ChatterLabelProblem.IconMissing in problems -> R.string.chatter_labels_error_icon_missing
        else -> null
    }
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.weight(1f),
        enabled = enabled,
        label = { Text(stringResource(R.string.chatter_labels_name)) },
        supportingText = problemText?.let { { Text(stringResource(it)) } },
        isError = problemText != null,
        singleLine = true,
    )
}

/** The label's image, or a plus while it has none; a tap picks a new one. */
@Composable
private fun LabelIconButton(icon: String, missing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val image = remember(icon) { decodeChatterLabelIcon(icon) }
    val description = stringResource(R.string.chatter_labels_choose_icon)
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(top = 4.dp)) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = description,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                Icons.Outlined.Add,
                contentDescription = description,
                tint = if (missing) MaterialTheme.colorScheme.error else LocalContentColor.current,
            )
        }
    }
}
