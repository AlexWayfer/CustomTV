package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatEventKind
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.chatMessageCanBeMentioned
import name.alexwayfer.customtv.chat.chatMessageCanBeReplied
import name.alexwayfer.customtv.chat.copyableText
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.data.sharedChatSourceOnUserMessage
import name.alexwayfer.customtv.ui.theme.ChatFirstMessage
import name.alexwayfer.customtv.ui.theme.ChatFirstTimeChatter
import name.alexwayfer.customtv.ui.theme.ChatMention
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

private const val CHAT_SELECTED_OVERLAY_ALPHA = 0.24f

/** Events drawn with a colored stripe along the row's start edge, as a cheer is. */
private val StripedEventKinds = setOf(
    ChatEventKind.Reward,
    ChatEventKind.Subscription,
    ChatEventKind.WatchStreak,
    ChatEventKind.Announcement,
)

@Composable
internal fun ChatRow(
    message: ChatMessage,
    settings: ChatListSettings,
    place: ChatRowPlace,
    selected: Boolean,
    actions: ChatRowActions,
) {
    val appearance = settings.appearance
    val sizes = chatRowSizes(appearance.textSize)
    if (chatOwnLine(message, appearance, sizes, settings.showTimestamps, stripeColor = settings.highlightColor)) return
    // Remembered, so a row composed again runs the text match once.
    val mentioned = remember(message, settings.mentions) { settings.mentions.matches(message) }
    val displayMessage = rememberDisplayMessage(message, appearance)
    // Read on each frame by the scroll that keeps the row in sight, not by the composition.
    val replyFolding = remember { mutableStateOf(false) }
    KeepSelectedMessageVisible(selected, message.id, place) { replyFolding.value }
    val onReply = actions.onReply?.takeIf { chatMessageCanBeReplied(message) }
    val onMention = LocalOnChatMention.current?.takeIf { chatMessageCanBeMentioned(message) }
    val look = ChatRowLook(
        appearance = appearance,
        sizes = sizes,
        timestampText = rememberChatTimestamp(message.timestampMillis, settings.showTimestamps),
        accent = chatRowAccent(message, displayMessage),
        pointsIconUrl = settings.channelPointsIconUrl,
        highlightRewardCost = settings.highlightRewardCost,
    )
    val copyText = remember(message) { message.copyableText() }
    ChatRowSwipe(message, copyText, onReply, actions) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(chatRowBackground(message, mentioned, settings))
                .chatRowStripe(chatRowStripeColor(message, settings, look.accent))
                .background(animatedSelectionOverlay(selected, CHAT_SELECTED_OVERLAY_ALPHA))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClickLabel = stringResource(
                        if (onReply != null || onMention != null) R.string.chat_message_actions else R.string.chat_copy,
                    ),
                    // Every long press that selects the message vibrates by itself.
                    onLongClick = actions.onSelect,
                    onClick = { if (selected) actions.onDismissSelection(ChatRowHoldEnd.Dismiss) },
                ),
        ) {
            val deletedAppearance = deletedMessageAppearance(displayMessage.deleted)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
            ) {
                Column(modifier = Modifier.alpha(animatedContentAlpha(deletedAppearance.contentAlpha))) {
                    ChatRowHeaders(message, look)
                    ChatRowEventLine(message, look)
                    if (chatRowShowsUserMessage(message)) {
                        ChatRowUserMessage(message, displayMessage, look, selected, actions.onSelect) {
                            replyFolding.value = it
                        }
                    }
                }
                ChatRowLinkPreview(message.rawText, settings.linkPreviewMode, deletedAppearance.linkPreviewAlpha)
                if (selected) ChatRowMenu(message, copyText, onReply, onMention, actions.onDismissSelection)
            }
        }
    }
}

/**
 * The row's swipes to reply and to copy [copyText], which tell the list when one starts and how it ends. Full screen
 * leaves both to the chat's own sideways swipe.
 */
@Composable
private fun ChatRowSwipe(
    message: ChatMessage,
    copyText: String?,
    onReply: ((ChatMessage) -> Unit)?,
    actions: ChatRowActions,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val fullscreen = LocalPlayerContentFullscreen.current
    val onReplySwipe = onReply?.takeIf { !fullscreen }?.let { reply -> { reply(message) } }
    val onCopySwipe = copyText?.takeIf { actions.copySwipe && chatSwipeCopies(it, fullscreen) }
        ?.let { text -> { context.copyChatText(text) } }
    ChatReplySwipe(
        enabled = onReplySwipe != null || onCopySwipe != null,
        onReply = onReplySwipe,
        onSwipingChange = actions.onReplySwipingChange,
        onCopy = onCopySwipe,
        onSwipeEnd = actions.onReplySwipeEnd,
        content = content,
    )
}

/** The message's time when timestamps are shown. */
@Composable
private fun rememberChatTimestamp(timestampMillis: Long, shown: Boolean): String? {
    val context = LocalContext.current
    return remember(timestampMillis, shown) {
        if (shown) formatChatTimestamp(context, timestampMillis) else null
    }
}

/** A notice, an AutoMod notice, or an emote change, drawn in a line of its own; false for any other message. */
@Composable
private fun chatOwnLine(
    message: ChatMessage,
    appearance: ChatAppearance,
    sizes: ChatRowSizes,
    showTimestamps: Boolean,
    stripeColor: Color,
): Boolean {
    val notice = message.notice
    val autoModNotice = message.autoModNotice
    val emoteChange = message.emoteChange
    when {
        notice != null -> ChatNoticeLine(notice, sizes.text, sizes.lineHeight)
        autoModNotice != null -> {
            val iconSize = chatDp(sizes.chatTextSize, normal = 14, large = 16)
            ChatAutoModNoticeLine(autoModNotice, iconSize, sizes.text, sizes.lineHeight)
        }
        emoteChange != null -> ChatEmoteChangeLine(
            message = message,
            change = emoteChange,
            readableColors = appearance.readableColors,
            stripeColor = stripeColor,
            showTimestamps = showTimestamps,
            textSize = sizes.text,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
            emoteSize = sizes.emote,
            logo = appearance.platformLogos[emoteChange.platform],
        )
        else -> return false
    }
    return true
}

/** An announcement takes the channel's color when Twitch sends the plain one. */
private fun chatRowAccent(message: ChatMessage, displayMessage: ChatMessage): Color {
    val channelAccent = displayMessage.color.takeIf { it != Color.Unspecified } ?: TwitchPurple
    return if (message.eventKind == ChatEventKind.Announcement && displayMessage.accentColor == TwitchTextSecondary) {
        channelAccent
    } else {
        displayMessage.accentColor ?: TwitchPurple
    }
}

private fun chatRowBackground(message: ChatMessage, mentioned: Boolean, settings: ChatListSettings): Color = when {
    mentioned -> ChatMention
    message.eventKind == ChatEventKind.Highlight -> settings.highlightColor.copy(alpha = 0.22f)
    message.firstInChannel -> ChatFirstTimeChatter
    settings.highlightFirstMessages && message.firstInSession -> ChatFirstMessage
    else -> Color.Transparent
}

/** The color of the stripe along the row's start edge; null for a row without one. */
private fun chatRowStripeColor(message: ChatMessage, settings: ChatListSettings, accent: Color): Color? = when {
    message.cheerBits != null -> settings.channelColor
    message.eventKind !in StripedEventKinds -> null
    message.eventKind == ChatEventKind.Announcement -> accent
    else -> settings.highlightColor
}

private fun Modifier.chatRowStripe(color: Color?): Modifier = if (color != null) chatStartStripe(color) else this

private fun chatRowShowsUserMessage(message: ChatMessage): Boolean {
    val shown = message.parts.isNotEmpty() ||
        message.eventKind == ChatEventKind.Announcement ||
        message.eventKind == ChatEventKind.Highlight ||
        message.eventKind == ChatEventKind.Normal
    return shown && message.displayName.isNotEmpty()
}

/**
 * The chatter's message, under the message it replies to; holding an emote in it selects the row. [onReplyFoldChange]
 * reports while the replied message unfolds or folds.
 */
@Composable
private fun ChatRowUserMessage(
    message: ChatMessage,
    displayMessage: ChatMessage,
    look: ChatRowLook,
    selected: Boolean,
    onSelect: () -> Unit,
    onReplyFoldChange: (Boolean) -> Unit,
) {
    val sizes = look.sizes
    val appearance = look.appearance
    ChatModerationFrame(
        message = message,
        iconSize = chatDp(sizes.chatTextSize, normal = 14, large = 16),
        textSize = sizes.timestamp,
    ) {
        message.reply?.let { reply ->
            ChatReplyPreview(
                reply = reply,
                chatTextSize = sizes.chatTextSize,
                meMessageItalic = appearance.meMessageItalic,
                expanded = selected,
                onFoldChange = onReplyFoldChange,
            )
        }
        ChatUserMessage(
            message = displayMessage,
            readableColors = appearance.readableColors,
            timestampText = look.timestampText.takeIf {
                if (message.watchStreak != null) {
                    watchStreakTimestampOnUserMessage(message.parts.isNotEmpty())
                } else {
                    message.systemText.isNullOrBlank()
                }
            },
            textSize = sizes.text,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
            emoteSize = sizes.emote,
            gifSize = sizes.gif,
            badgeSize = sizes.badge,
            badgeUrls = appearance.badgeUrls,
            meMessageItalic = appearance.meMessageItalic,
            showSharedChatSource = sharedChatSourceOnUserMessage(message.systemText),
            onEmoteLongClick = onSelect,
        )
    }
}

/**
 * Reply, Mention, and Copy over a selected row, when the message can be replied to, has a chatter to mention, or has
 * text to copy.
 */
@Composable
private fun ChatRowMenu(
    message: ChatMessage,
    copyText: String?,
    onReply: ((ChatMessage) -> Unit)?,
    onMention: ((login: String, displayName: String) -> Unit)?,
    onDismissSelection: (ChatRowHoldEnd) -> Unit,
) {
    val context = LocalContext.current
    if (copyText == null && onReply == null && onMention == null) return
    ChatMessageCopyPopup(
        onReply = onReply?.let { reply ->
            {
                reply(message)
                onDismissSelection(ChatRowHoldEnd.Reply)
            }
        },
        onMention = onMention?.let { mention ->
            {
                mention(message.userLogin, message.displayName)
                onDismissSelection(ChatRowHoldEnd.Mention)
            }
        },
        onCopy = copyText?.let { text ->
            {
                context.copyChatText(text)
                onDismissSelection(ChatRowHoldEnd.Copy)
            }
        },
        onDismiss = { onDismissSelection(ChatRowHoldEnd.Dismiss) },
    )
}
