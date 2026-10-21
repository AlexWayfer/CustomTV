package name.alexwayfer.customtv.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatterLabelIcon
import name.alexwayfer.customtv.data.ChatterLabelsRepository
import name.alexwayfer.customtv.ui.watch.ChatterLabelsSheet

/**
 * The channel's labels, each icon with its name, or that it has none, while Chatter Labels is connected. A tap on
 * the line opens the labels to change them.
 */
@Composable
internal fun ProfileChatterLabels(userId: String?, login: String, displayName: String) {
    val credentials by ChatterLabelsRepository.credentials.collectAsStateWithLifecycle()
    val labelsByUserId by ChatterLabelsRepository.labelsByUserId.collectAsStateWithLifecycle()
    var editing by remember(userId) { mutableStateOf(false) }
    val labels = userId?.let(labelsByUserId::get).orEmpty()
    AnimatedVisibility(visible = credentials != null && userId != null) {
        // The tap target reaches past the line, so the line sits 4 dp lower than the 16 dp it shows above.
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(
                    onClickLabel = stringResource(R.string.chatter_labels_edit),
                    role = Role.Button,
                    onClick = { editing = true },
                )
                .padding(vertical = 12.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (labels.isEmpty()) {
                Text(
                    text = stringResource(R.string.chatter_card_no_labels),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    labels.forEach { label -> ProfileChatterLabel(label) }
                }
            }
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (editing && userId != null) {
        ChatterLabelsSheet(
            userId = userId,
            login = login,
            displayName = displayName,
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun ProfileChatterLabel(label: ChatterLabelIcon) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = label.image,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = label.name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
