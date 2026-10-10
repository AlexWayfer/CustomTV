package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatModeButton
import name.alexwayfer.customtv.chat.ChatModePlaque
import name.alexwayfer.customtv.chat.chatModeButton
import name.alexwayfer.customtv.chat.chatModeCountdown
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** One line of active chat modes on the input's own background; a tap opens their details in a sheet. */
@Composable
internal fun ChatModePlaqueList(
    plaques: List<ChatModePlaque>,
    signedIn: Boolean,
    onFollow: () -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (plaques.isEmpty()) return
    val sheetKeyboard = LocalSheetKeyboard.current
    var detailsOpen by remember { mutableStateOf(false) }
    val titles = plaques.map { stringResource(it.titleRes()) }
    val detailsLabel = stringResource(R.string.chat_modes_details)
    val openDetails = { sheetKeyboard.open { detailsOpen = true } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = openDetails)
            .clearAndSetSemantics {
                contentDescription = titles.joinToString(", ")
                role = Role.Button
                onClick(label = detailsLabel) { openDetails(); true }
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TwitchTextSecondary,
        )
        Text(
            text = titles.joinToString(" · "),
            color = TwitchText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (detailsOpen) {
        ChatModeDetailsSheet(
            plaques = plaques,
            signedIn = signedIn,
            onFollow = onFollow,
            onSubscribe = onSubscribe,
            onDismiss = {
                detailsOpen = false
                sheetKeyboard.onDismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatModeDetailsSheet(
    plaques: List<ChatModePlaque>,
    signedIn: Boolean,
    onFollow: () -> Unit,
    onSubscribe: () -> Unit,
    onDismiss: () -> Unit,
) {
    DismissWhenPlayerContentHidden(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_modes_title),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            plaques.forEach { plaque ->
                val actionLabel = when (chatModeButton(plaque, signedIn)) {
                    ChatModeButton.Follow -> R.string.chat_mode_follow
                    ChatModeButton.Subscribe -> R.string.chat_mode_subscribe
                    ChatModeButton.LogIn -> R.string.log_in
                    null -> null
                }
                ListItem(
                    headlineContent = { Text(stringResource(plaque.titleRes())) },
                    supportingContent = { Text(plaque.detail(), color = TwitchTextSecondary) },
                    trailingContent = actionLabel?.let { label ->
                        {
                            Button(
                                onClick = {
                                    onDismiss()
                                    if (plaque is ChatModePlaque.FollowersNeedFollow) onFollow() else onSubscribe()
                                },
                            ) {
                                Text(stringResource(label))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun ChatModePlaque.detail(): String = when (this) {
    is ChatModePlaque.FollowersNeedFollow -> stringResource(R.string.chat_mode_followers_follow, channel)
    ChatModePlaque.FollowersRoom -> stringResource(R.string.chat_mode_followers_body)
    is ChatModePlaque.FollowersWait -> stringResource(
        R.string.chat_mode_wait,
        chatModeCountdown(remainingMillis),
    )
    is ChatModePlaque.Subscribers -> stringResource(R.string.chat_mode_subscribers_body)
    ChatModePlaque.Emotes -> stringResource(R.string.chat_mode_emotes_body)
    is ChatModePlaque.Slow -> remainingMillis?.let { remaining ->
        stringResource(R.string.chat_mode_wait, chatModeCountdown(remaining))
    } ?: pluralStringResource(R.plurals.chat_mode_slow_every, seconds, seconds)
}

private fun ChatModePlaque.titleRes(): Int = when (this) {
    is ChatModePlaque.FollowersNeedFollow, is ChatModePlaque.FollowersWait, ChatModePlaque.FollowersRoom ->
        R.string.chat_mode_followers
    is ChatModePlaque.Subscribers -> R.string.chat_mode_subscribers
    ChatModePlaque.Emotes -> R.string.chat_mode_emotes
    is ChatModePlaque.Slow -> R.string.chat_mode_slow
}
