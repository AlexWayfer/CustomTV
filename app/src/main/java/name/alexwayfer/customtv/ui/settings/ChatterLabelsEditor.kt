package name.alexwayfer.customtv.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.data.ChatterLabelProblem
import name.alexwayfer.customtv.data.EditableChatterLabel
import name.alexwayfer.customtv.data.ChatterLabelsCredentials
import name.alexwayfer.customtv.data.readChatterLabelIcon
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.components.ChannelPreview
import name.alexwayfer.customtv.ui.components.SearchField
import name.alexwayfer.customtv.ui.components.TextButton
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import name.alexwayfer.customtv.ui.rememberHeldWhileLoading
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableColumnScope

private enum class LabelsPhase { Loading, Failed, Ready }

/**
 * The labels of the connected Gist to add, rename, reorder, hide, and delete. The page reads the Gist each time it
 * opens; Save writes every change at once, and leaving with changes that are not saved asks first.
 */
@Composable
internal fun ChatterLabelsEditor(
    credentials: ChatterLabelsCredentials,
    state: ChatterLabelsEditorState,
    leaveGuard: SettingsLeaveGuard,
    onOpenProfile: (ChannelPreview) -> Unit,
    onNotice: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(credentials) { state.load(credentials) }

    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val changed = state.changed
    DisposableEffect(leaveGuard, changed) {
        leaveGuard.onLeave = if (changed) { leave -> pendingLeave = leave } else null
        onDispose { leaveGuard.onLeave = null }
    }
    var deletingId by remember { mutableStateOf<String?>(null) }
    val chooseIcon = rememberLabelIconPicker(state)

    SettingsSection(stringResource(R.string.chatter_labels_section))
    val phase = when {
        state.ready -> LabelsPhase.Ready
        state.loadError != null -> LabelsPhase.Failed
        else -> LabelsPhase.Loading
    }
    val shownPhase = rememberHeldWhileLoading(phase, loading = phase == LabelsPhase.Loading)
    AnimatedContent(
        targetState = shownPhase,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "labelsPhase",
    ) { current ->
        when (current) {
            LabelsPhase.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            LabelsPhase.Failed -> LabelsLoadFailed(state.loadError) { scope.launch { state.load(credentials) } }
            LabelsPhase.Ready -> ChatterLabelsList(
                state = state,
                onChooseIcon = chooseIcon,
                onDelete = { id -> if (state.isSaved(id)) deletingId = id else state.delete(id) },
                onChatterTap = rememberChatterTap(state, onOpenProfile, onNotice),
            )
        }
    }

    deletingId?.let { id -> state.labels.find { it.id == id } }?.let { label ->
        ChatterLabelDeleteDialog(
            name = label.name,
            chatters = state.assignees[label.id]?.size ?: 0,
            onConfirm = {
                deletingId = null
                state.delete(label.id)
            },
            onDismiss = { deletingId = null },
        )
    }
    pendingLeave?.let { leave ->
        ChatterLabelsDiscardDialog(
            onDiscard = {
                pendingLeave = null
                leave()
            },
            onStay = { pendingLeave = null },
        )
    }
}

/** Picks an image for the label with the given ID and puts it on the label, or tells when it cannot be read. */
@Composable
private fun rememberLabelIconPicker(state: ChatterLabelsEditorState): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pickingIconFor by remember { mutableStateOf<String?>(null) }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = pickingIconFor
        pickingIconFor = null
        if (uri == null || id == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val icon = withContext(Dispatchers.IO) { readChatterLabelIcon(context, uri) }
                state.update(id) { it.copy(icon = icon) }
            } catch (_: IOException) {
                state.error = UiText.Resource(R.string.chatter_labels_error_icon_read)
            }
        }
    }
    return { id ->
        pickingIconFor = id
        iconPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
private fun LabelsLoadFailed(error: UiText?, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = error?.asString().orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * The labels to drag into a new order and edit, and Add under them. A search by nick above them leaves only the
 * labels of the chatters that match, with those chatters listed; the order and Add wait until it is cleared.
 */
@Composable
private fun ChatterLabelsList(
    state: ChatterLabelsEditorState,
    onChooseIcon: (String) -> Unit,
    onDelete: (String) -> Unit,
    onChatterTap: (ChatterLabelAssignee) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var query by rememberSaveable { mutableStateOf("") }
    val term = chatterSearchTerm(query)
    val searching = term != null
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        val labels = state.labels
        // Shown while something is typed too, so the search can be cleared after its last label is deleted.
        if (labels.isNotEmpty() || query.isNotEmpty()) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = R.string.chatter_labels_search,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                contentPadding = ChatterLabelFieldPadding,
            )
        }
        if (labels.isEmpty()) {
            Text(
                text = stringResource(R.string.chatter_labels_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val problems = if (state.showProblems) state.problems else emptyMap()
        val labelsById = labels.associateBy { it.id }
        val shownAssignees = shownAssignees(state, term)
        ReorderableColumn(
            // The IDs, not the labels: the list's drag state starts over whenever the list changes, and a typed
            // letter must not do that.
            list = labels.map { it.id },
            onSettle = { from, to -> state.move(from, to) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            onMove = { haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
        ) { index, id, dragging ->
            val label = labelsById[id] ?: return@ReorderableColumn
            key(id) {
                LabelListItem(
                    state = state,
                    label = label,
                    index = index,
                    lastIndex = labels.lastIndex,
                    assignees = shownAssignees[id].orEmpty(),
                    problems = problems[id].orEmpty(),
                    dragging = dragging,
                    searching = searching,
                    onChooseIcon = onChooseIcon,
                    onDelete = onDelete,
                    onChatterTap = onChatterTap,
                )
            }
        }
        LabelsListFooter(
            searching = searching,
            nothingFound = searching && labels.none { shownAssignees[it.id].orEmpty().isNotEmpty() },
            saving = state.saving,
            onAdd = state::add,
        )
    }
}

/** The chatters each label lists, by label ID: the ones that match [term], and none for a label not saved yet. */
private fun shownAssignees(state: ChatterLabelsEditorState, term: String?): Map<String, List<ChatterLabelAssignee>> =
    state.labels.associate { label ->
        val all = if (state.isSaved(label.id)) state.assignees[label.id].orEmpty() else emptyList()
        label.id to assigneesMatching(all, term)
    }

/**
 * A label in the list, which the search hides when none of its chatters match, and which it cannot move meanwhile.
 * The gap above it goes with it, so a hidden label leaves none.
 */
@Composable
private fun ReorderableColumnScope.LabelListItem(
    state: ChatterLabelsEditorState,
    label: EditableChatterLabel,
    index: Int,
    lastIndex: Int,
    assignees: List<ChatterLabelAssignee>,
    problems: Set<ChatterLabelProblem>,
    dragging: Boolean,
    searching: Boolean,
    onChooseIcon: (String) -> Unit,
    onDelete: (String) -> Unit,
    onChatterTap: (ChatterLabelAssignee) -> Unit,
) {
    val id = label.id
    // Outside the hiding, as the item takes its place from its own position in the list.
    ReorderableItem {
        AnimatedVisibility(
            visible = !searching || assignees.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(top = if (index > 0) 8.dp else 0.dp)) {
                ChatterLabelCard(
                    label = label,
                    assignees = assignees,
                    problems = problems,
                    dragging = dragging,
                    enabled = !state.saving,
                    reorderable = !searching,
                    searching = searching,
                    onMoveUp = if (index > 0 && !searching) ({ state.move(index, index - 1) }) else null,
                    onMoveDown = if (index < lastIndex && !searching) ({ state.move(index, index + 1) }) else null,
                    onChooseIcon = { onChooseIcon(id) },
                    onNameChange = { name -> state.update(id) { it.copy(name = name) } },
                    onEnabledChange = { enabled -> state.update(id) { it.copy(enabled = enabled) } },
                    onDelete = { onDelete(id) },
                    onChatterTap = onChatterTap,
                )
            }
        }
    }
}

/** Under the labels: that the search found none, or Add, which waits until the search is cleared. */
@Composable
private fun ColumnScope.LabelsListFooter(
    searching: Boolean,
    nothingFound: Boolean,
    saving: Boolean,
    onAdd: () -> Unit,
) {
    AnimatedVisibility(visible = nothingFound, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = stringResource(R.string.chatter_labels_search_empty),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    AnimatedVisibility(visible = !searching, enter = fadeIn(), exit = fadeOut()) {
        TextButton(
            onClick = onAdd,
            enabled = !saving,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.chatter_labels_add))
        }
    }
}

/** Save, floating over the page while there are changes, with the last failure above it. */
@Composable
internal fun ChatterLabelsSaveButton(state: ChatterLabelsEditorState, visible: Boolean, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        val error = state.error
        val shownError = rememberLastNonNull(error)
        AnimatedVisibility(
            // An image that cannot be read fails before anything changed, so the failure shows without Save too.
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = shownError?.asString().orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        AnimatedVisibility(visible = visible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            ExtendedFloatingActionButton(
                onClick = { if (!state.saving) scope.launch { state.save() } },
                icon = {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.chatter_labels_save)) },
            )
        }
    }
}
