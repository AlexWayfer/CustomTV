package name.alexwayfer.customtv.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.components.TextButton
import name.alexwayfer.customtv.update.AppReleaseNotes

@Composable
internal fun AppUpdateNotes(notes: AppReleaseNotes, expanded: Boolean, onToggle: () -> Unit) {
    val entries = notes.forLanguage(LocalConfiguration.current.locales[0].language)
    if (entries.isEmpty()) return
    val state = stringResource(
        if (expanded) R.string.app_update_notes_expanded else R.string.app_update_notes_collapsed,
    )
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "app update notes arrow")
    Column {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.padding(vertical = 4.dp).semantics { stateDescription = state },
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.app_update_notes))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.forEach { AppUpdateNote(it) }
            }
        }
    }
}

@Composable
private fun AppUpdateNote(entry: String) {
    val color = MaterialTheme.colorScheme.onSurface
    val style = MaterialTheme.typography.bodyLarge
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", modifier = Modifier.clearAndSetSemantics {}, color = color, style = style)
        Text(entry, color = color, style = style)
    }
}
