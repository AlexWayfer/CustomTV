package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelAssignee
import name.alexwayfer.customtv.ui.watch.NoticeExpandArrow
import name.alexwayfer.customtv.ui.watch.formatChatterLabelAssignedAt

/** The list's height limit in lines of its text, as the extension limits its list to 18em. */
private const val ASSIGNEES_MAX_HEIGHT_EM = 18

/** Whether chat shows the label; the whole row switches it. */
@Composable
internal fun ChatterLabelShownInChat(shown: Boolean, enabled: Boolean, onShownChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chatter_labels_show_in_chat)) },
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

/**
 * How many chatters have the label, in a row a tap on which lists them under it, in a list of limited height that
 * scrolls; a tap on a chatter goes to [onChatterTap], and Delete in the menu a long press opens to [onChatterDelete].
 * While [searching], [assignees] are the chatters that match, and they are listed at once. A label without chatters
 * shows nothing.
 */
@Composable
internal fun ChatterLabelAssignees(
    assignees: List<ChatterLabelAssignee>,
    searching: Boolean,
    enabled: Boolean,
    onChatterTap: (ChatterLabelAssignee) -> Unit,
    onChatterDelete: (ChatterLabelAssignee) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    AnimatedVisibility(
        visible = assignees.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            AssigneesCount(
                count = assignees.size,
                expanded = expanded,
                onToggle = if (searching) null else ({ expanded = !expanded }),
            )
            AnimatedVisibility(
                visible = expanded || searching,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                // A save under way would stop as the page leaves for the profile.
                AssigneeList(assignees, enabled, onChatterTap, onChatterDelete)
            }
        }
    }
}

/**
 * How many chatters have the label, with an arrow that shows the list opens from the row; without [onToggle], as
 * while searching, how many of them match, and the list stays open.
 */
@Composable
private fun AssigneesCount(count: Int, expanded: Boolean, onToggle: (() -> Unit)?) {
    ListItem(
        headlineContent = {
            Text(
                text = if (onToggle != null) {
                    pluralStringResource(R.plurals.chatter_labels_chatters, count, count)
                } else {
                    pluralStringResource(R.plurals.chatter_labels_chatters_found, count, count)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = onToggle?.let {
            {
                NoticeExpandArrow(
                    expanded = expanded,
                    contentDescription = stringResource(
                        if (expanded) R.string.chatter_labels_chatters_hide else R.string.chatter_labels_chatters_show,
                    ),
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onToggle != null) Modifier.clickable(role = Role.Button, onClick = onToggle) else Modifier,
    )
}

/**
 * Each chatter's name after a bullet, and the date the label was assigned when the Gist has it; outlined once it
 * scrolls. A fling stops at the list's end instead of scrolling the page on.
 */
@Composable
private fun AssigneeList(
    assignees: List<ChatterLabelAssignee>,
    enabled: Boolean,
    onChatterTap: (ChatterLabelAssignee) -> Unit,
    onChatterDelete: (ChatterLabelAssignee) -> Unit,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val maxHeight = with(LocalDensity.current) { (textStyle.fontSize * ASSIGNEES_MAX_HEIGHT_EM).toDp() }
    val listState = rememberLazyListState()
    val flingStop = rememberListEndFlingStop(listState)
    val scrolls = listState.canScrollForward || listState.canScrollBackward
    LazyColumn(
        state = listState,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .then(
                if (scrolls) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = if (scrolls) 4.dp else 0.dp, vertical = if (scrolls) 4.dp else 0.dp)
            .nestedScroll(flingStop.connection),
        flingBehavior = flingStop.flingBehavior,
    ) {
        items(assignees, key = { it.userId }) { assignee ->
            AssigneeRow(
                assignee = assignee,
                textStyle = textStyle,
                enabled = enabled,
                onTap = { onChatterTap(assignee) },
                onDelete = { onChatterDelete(assignee) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/**
 * A chatter in the list; a tap opens their profile, or tells why it cannot, and a long press opens a menu with Delete,
 * which TalkBack offers as an action.
 */
@Composable
private fun AssigneeRow(
    assignee: ChatterLabelAssignee,
    textStyle: TextStyle,
    enabled: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val openLabel = stringResource(R.string.chatter_card_open_profile)
    val deleteLabel = stringResource(R.string.chatter_labels_delete)
    Box(modifier = modifier) {
        AssigneeRowContent(
            assignee = assignee,
            textStyle = textStyle,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    enabled = enabled,
                    onClickLabel = openLabel,
                    role = Role.Button,
                    // The long press vibrates by itself.
                    onLongClick = { menuOpen = true },
                    onClick = onTap,
                )
                .semantics {
                    if (enabled) customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                },
        )
        DropdownMenu(expanded = menuOpen && enabled, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(deleteLabel) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/** The chatter's name after a bullet, and the date the label was assigned when the Gist has it. */
@Composable
private fun AssigneeRowContent(assignee: ChatterLabelAssignee, textStyle: TextStyle, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dateColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dateSize = MaterialTheme.typography.bodySmall.fontSize
    val lineHeight = with(LocalDensity.current) { textStyle.lineHeight.toDp() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The bullet sits in the middle of the first line, as a list's disc does.
        Box(modifier = Modifier.height(lineHeight), contentAlignment = Alignment.Center) {
            Box(Modifier.size(5.dp).background(dateColor, CircleShape))
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(assignee.shownName) }
                assignee.assignedAtMillis?.let { millis ->
                    append("  ")
                    withStyle(SpanStyle(color = dateColor, fontSize = dateSize)) {
                        append(formatChatterLabelAssignedAt(millis, locale))
                    }
                }
            },
            style = textStyle,
        )
    }
}
