package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatMessage

@Suppress("unused")
@Composable
internal fun rememberChatterModTools(userId: String?, login: String?, badges: List<ChatBadge>?): ChatterModTools {
    val locked = chatterCardModerationLocked(
        ownBadges = LocalOwnChatBadges.current,
        broadcasterId = LocalEmotePreviewChannel.current.twitchUserId,
        targetUserId = userId,
    )
    return remember(locked) { ChatterModTools(locked) }
}

/** The free chatter card shows Moderation locked to the channel's moderators; a tap shows the Premium banner. */
@Suppress("unused")
@Composable
internal fun ChatterCardModeration(
    tools: ChatterModTools,
    displayName: String,
    message: ChatMessage?,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onOpenLogs: (tab: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!tools.locked) return
    ChatterCardLockedItem(
        icon = painterResource(R.drawable.ic_moderation_shield_outline),
        title = stringResource(R.string.moderation_section),
        source = "the chatter card's moderation",
        modifier = modifier,
    )
}

/** The free chatter card never opens the moderation logs. */
@Suppress("unused")
@Composable
internal fun ChatterCardModLogsPage(
    tools: ChatterModTools,
    initialTab: Int,
    renderMessage: @Composable (ChatMessage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
