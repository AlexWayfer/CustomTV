package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.displayNameLabel
import name.alexwayfer.customtv.channel.displayNameLoginSuffix
import name.alexwayfer.customtv.data.CollaborationChannel
import name.alexwayfer.customtv.data.CollaborationRepository
import name.alexwayfer.customtv.data.CollaborationResult
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.collaborationRowOpensChannel
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.theme.TwitchLive
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

@Composable
internal fun rememberCollaboration(
    broadcasterId: String?,
    currentLogin: String,
): CollaborationResult? {
    val repository = remember { CollaborationRepository() }
    var result by remember(broadcasterId, currentLogin) { mutableStateOf<CollaborationResult?>(null) }
    LaunchedEffect(broadcasterId, currentLogin) {
        if (broadcasterId.isNullOrBlank()) return@LaunchedEffect
        result = repository.load(broadcasterId, currentLogin)
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollaborationSheet(
    result: CollaborationResult?,
    sharedViewerCount: Int?,
    currentLogin: String,
    onOpenChannel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = stringResource(R.string.collaboration_title)
    val sharedLabel = sharedViewerCount?.let { NumberFormat.getIntegerInstance().format(it) }
    val titleDescription = if (sharedLabel == null) {
        title
    } else {
        stringResource(R.string.collaboration_title_with_viewers, title, sharedLabel)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .clearAndSetSemantics { contentDescription = titleDescription },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (sharedLabel != null) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TwitchLive))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sharedLabel,
                        color = TwitchText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            when (result) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                CollaborationResult.Unavailable -> {
                    Text(
                        text = stringResource(R.string.collaboration_unavailable),
                        color = TwitchTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                is CollaborationResult.Ready -> {
                    if (result.channels.isEmpty()) {
                        Text(
                            text = stringResource(R.string.collaboration_empty),
                            color = TwitchTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    } else {
                        result.channels.forEach { channel ->
                            CollaborationChannelRow(channel, currentLogin, onOpenChannel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollaborationChannelRow(
    channel: CollaborationChannel,
    currentLogin: String,
    onOpenChannel: (String) -> Unit,
) {
    val label = displayNameLabel(channel.displayName, channel.login)
    val suffix = displayNameLoginSuffix(channel.displayName, channel.login)
    val viewers = channel.viewerCount?.let { NumberFormat.getIntegerInstance().format(it) }
    val description = if (viewers != null) {
        stringResource(R.string.collaboration_channel_live, label, viewers)
    } else {
        stringResource(R.string.collaboration_channel_offline, label)
    }
    val opensChannel = collaborationRowOpensChannel(channel.login, currentLogin)
    ListItem(
        modifier = Modifier
            .then(
                if (opensChannel) {
                    Modifier.clickable(onClick = { onOpenChannel(channel.login) })
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                if (opensChannel) role = Role.Button
                contentDescription = description
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            ChannelAvatar(
                channel = channel.login,
                profile = ChannelProfile(
                    login = channel.login,
                    displayName = channel.displayName,
                    avatarUrl = channel.avatarUrl,
                ),
                size = 40.dp,
            )
        },
        headlineContent = {
            Text(channel.displayName, color = TwitchText, fontWeight = FontWeight.Medium)
        },
        supportingContent = if (suffix == null) {
            null
        } else {
            { Text(suffix, color = TwitchTextSecondary, fontSize = 12.sp) }
        },
        trailingContent = if (viewers == null) {
            null
        } else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TwitchLive))
                    Spacer(Modifier.width(6.dp))
                    Text(viewers, color = TwitchText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
    )
}
