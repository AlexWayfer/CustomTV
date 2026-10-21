package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.data.chatterLabelsErrorText
import name.alexwayfer.customtv.ui.UiText
import name.alexwayfer.customtv.ui.asString
import name.alexwayfer.customtv.ui.components.TextButton
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
import org.json.JSONException

/** The chatter's labels on the card, with the way to change them, while Chatter Labels is connected. */
@Composable
internal fun ChatterCardLabels(userId: String?, onManage: () -> Unit) {
    val credentials by ChatterLabelsRepository.credentials.collectAsStateWithLifecycle()
    if (credentials == null) return
    val labelsByUserId by ChatterLabelsRepository.labelsByUserId.collectAsStateWithLifecycle()
    val assigned = userId?.let(labelsByUserId::get).orEmpty()
    Text(
        text = stringResource(R.string.chatter_card_labels),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
    )
    if (assigned.isEmpty()) {
        Text(
            text = stringResource(R.string.chatter_card_no_labels),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    assigned.forEach { label ->
        ChatterLabelRow(label = label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
    }
    // The text lines up with the rows above; the button's own padding stays its touch target.
    TextButton(onClick = onManage, enabled = userId != null, modifier = Modifier.padding(start = 4.dp)) {
        Text(stringResource(R.string.chatter_card_labels_manage))
    }
}

/**
 * Every label, checked where the chatter has it; Save writes the change to the Gist. Without [onBack], as in a sheet
 * of its own, the title stands alone.
 */
@Composable
internal fun ChatterCardLabelsPage(
    userId: String,
    login: String,
    displayName: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val labels by ChatterLabelsRepository.labels.collectAsStateWithLifecycle()
    val labelsByUserId by ChatterLabelsRepository.labelsByUserId.collectAsStateWithLifecycle()
    val assigned = labelsByUserId[userId].orEmpty()
    val assignedIds = remember(assigned) { assigned.mapTo(mutableSetOf()) { it.id } }
    // A save that succeeded changes what the chatter has, and the checks start from it again.
    var checked by remember(assignedIds) { mutableStateOf<Set<String>>(assignedIds) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val overscroll = rememberOverscrollEffect()
    val stopAtTop = rememberStopAtScrollTop(scrollState, overscroll)
    Column(modifier = modifier.fillMaxWidth()) {
        LabelsPageHeader(onBack)
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .nestedScroll(stopAtTop)
                .verticalScroll(scrollState, overscrollEffect = overscroll),
        ) {
            labels.forEach { label ->
                LabelCheckRow(
                    // The date stays the saved one until Save, so a fresh check shows none yet.
                    label = assigned.find { it.id == label.id } ?: label,
                    checked = label.id in checked,
                    enabled = !saving,
                    onCheckedChange = { on -> checked = if (on) checked + label.id else checked - label.id },
                )
            }
        }
        LabelsSaveError(error)
        Button(
            onClick = {
                saving = true
                error = null
                val shownIds = labels.mapTo(mutableSetOf()) { it.id }
                val toSave = checked
                scope.launch {
                    try {
                        error = saveLabelAssignments(userId, login, displayName, shownIds, toSave)
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving && checked != assignedIds,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Text(stringResource(R.string.chatter_card_labels_save))
        }
    }
}

/** Saves which of the [shownIds] the chatter has; the reason when that failed, or null once saved. */
private suspend fun saveLabelAssignments(
    userId: String,
    login: String,
    displayName: String,
    shownIds: Set<String>,
    assigned: Set<String>,
): UiText? = try {
    ChatterLabelsRepository.saveAssignments(userId, login, displayName, shownIds, assigned)
    null
} catch (e: IOException) {
    e.chatterLabelsErrorText(UiText.Raw(e.javaClass.simpleName))
} catch (e: JSONException) {
    e.chatterLabelsErrorText(UiText.Raw(e.javaClass.simpleName))
}

/** Why the last save failed, keeping its text while it fades out. */
@Composable
private fun ColumnScope.LabelsSaveError(error: UiText?) {
    val shownError = rememberLastNonNull(error)
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Text(
            text = shownError?.asString().orEmpty(),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Back to the profile when there is one, and the page's title. */
@Composable
private fun LabelsPageHeader(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack == null) 16.dp else 0.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.chatter_card_back_to_profile),
                )
            }
        }
        Text(
            text = stringResource(R.string.chatter_card_labels),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A label with its checkbox; the whole row toggles it. */
@Composable
private fun LabelCheckRow(
    label: ChatterLabelIcon,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
}

/** A label's icon and name, with the time it was given to the chatter below when it was. */
@Composable
private fun ChatterLabelRow(label: ChatterLabelIcon, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = label.image,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit,
        )
        Column {
            Text(text = label.name, style = MaterialTheme.typography.bodyLarge)
            // A save gives a newly checked label its time and takes it from an unchecked one.
            val assignedAt = label.assignedAtMillis
            val shownAssignedAt = rememberLastNonNull(assignedAt)
            AnimatedVisibility(
                visible = assignedAt != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                shownAssignedAt?.let { millis ->
                    Text(
                        text = formatChatterLabelAssignedAt(millis, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun formatChatterLabelAssignedAt(millis: Long, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
