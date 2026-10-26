package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.data.ChatterLabelProblem
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.data.decodeChatterLabelIcon
import name.alexwayfer.customtv.ui.components.PaddedOutlinedTextField
import sh.calvin.reorderable.ReorderableListItemScope

/**
 * One label in the settings: its icon, name, and whether chat shows it, with a handle to hold and drag it while
 * [reorderable], and a way to delete it. [assignees] are the chatters who have it in the Gist, or the ones that match
 * while [searching]; a tap on one goes to [onChatterTap].
 */
@Composable
internal fun ReorderableListItemScope.ChatterLabelCard(
    label: EditableChatterLabel,
    assignees: List<ChatterLabelAssignee>,
    problems: Set<ChatterLabelProblem>,
    dragging: Boolean,
    enabled: Boolean,
    reorderable: Boolean,
    searching: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onChooseIcon: () -> Unit,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onChatterTap: (ChatterLabelAssignee) -> Unit,
) {
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
                LabelDragHandle(shown = reorderable, enabled = enabled)
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
            ChatterLabelShownInChat(label.enabled, enabled, onEnabledChange)
            ChatterLabelAssignees(assignees, searching, enabled, onChatterTap)
        }
    }
}

/**
 * The handle to drag the label by, which moves it only after a hold, so a scroll of the page that starts on it does
 * not; it slides away while the label cannot move.
 */
@Composable
private fun ReorderableListItemScope.LabelDragHandle(shown: Boolean, enabled: Boolean) {
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(
        visible = shown,
        enter = expandHorizontally() + fadeIn(),
        exit = shrinkHorizontally() + fadeOut(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = null,
            modifier = Modifier
                .longPressDraggableHandle(
                    enabled = enabled,
                    onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                    onDragStopped = { haptic.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                )
                .clearAndSetSemantics {}
                .padding(top = 8.dp)
                .padding(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    PaddedOutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        contentPadding = ChatterLabelFieldPadding,
        modifier = Modifier.weight(1f),
        enabled = enabled,
        label = { Text(stringResource(R.string.chatter_labels_name)) },
        supportingText = problemText?.let { { Text(stringResource(it)) } },
        isError = problemText != null,
    )
}

/** The padding of the label page's fields, which makes them 48dp high instead of Material's 56dp. */
internal val ChatterLabelFieldPadding = OutlinedTextFieldDefaults.contentPadding(top = 12.dp, bottom = 12.dp)

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
