package name.alexwayfer.customtv.ui.watch



















import androidx.compose.foundation.background

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row





import androidx.compose.foundation.layout.fillMaxWidth


import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size



import androidx.compose.foundation.lazy.LazyListState















import androidx.compose.material3.Text
import androidx.compose.material3.Icon




import androidx.compose.runtime.Composable



import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf


import androidx.compose.runtime.remember


import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment

import androidx.compose.ui.draw.alpha


import androidx.compose.ui.draw.drawBehind


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color



import androidx.compose.ui.hapticfeedback.HapticFeedbackType




import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalHapticFeedback


import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource










import androidx.compose.ui.text.font.FontWeight








import androidx.compose.ui.unit.LayoutDirection

import androidx.compose.ui.unit.dp













import name.alexwayfer.customtv.R



import name.alexwayfer.customtv.data.LinkPreviewMode
import name.alexwayfer.customtv.chat.ChatEventKind
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.chatMessageCanBeReplied
import name.alexwayfer.customtv.chat.ReadableChatColor




import name.alexwayfer.customtv.chat.copyableText



import name.alexwayfer.customtv.chat.withDisplayParts


import name.alexwayfer.customtv.chat.formatChatTimestamp






import name.alexwayfer.customtv.ui.theme.ChatFirstMessage
import name.alexwayfer.customtv.ui.theme.ChatFirstTimeChatter
import name.alexwayfer.customtv.ui.theme.ChatMention






import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchPurple


import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary


import java.util.concurrent.atomic.AtomicBoolean



private const val CHAT_SELECTED_OVERLAY_ALPHA = 0.24f

@Composable
internal fun ChatRow(
    message: ChatMessage,
    appearance: ChatAppearance,
    showTimestamps: Boolean,
    highlightColor: Color,
    channelColor: Color,
    highlightRewardCost: Int,
    channelPointsIconUrl: String?,
    highlightFirstMessages: Boolean,
    linkPreviewMode: LinkPreviewMode,
    mentioned: Boolean,
    listState: LazyListState,
    autoScrolling: AtomicBoolean,
    bottomOverlayPx: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onDismissSelection: () -> Unit,
    onReply: ((ChatMessage) -> Unit)?,
    onReplySwipingChange: (Boolean) -> Unit,
) {
    val readableColors = appearance.readableColors
    val chatTextSize = appearance.textSize
    val meMessageItalic = appearance.meMessageItalic
    val textSize = chatSp(chatTextSize, 13, 16)
    val timestampSize = chatSp(chatTextSize, 12, 14)
    val lineHeight = chatSp(chatTextSize, 21, 25)
    val emoteSize = chatDp(chatTextSize, 22, 26)
    val gifSize = chatDp(chatTextSize, 96, 112)
    val badgeSize = chatDp(chatTextSize, 16, 20)
    val notice = message.notice
    if (notice != null) {
        ChatNoticeLine(notice, textSize, lineHeight)
        return
    }
    message.autoModNotice?.let { autoModNotice ->
        ChatAutoModNoticeLine(autoModNotice, chatDp(chatTextSize, 14, 16), textSize, lineHeight)
        return
    }
    val emoteChange = message.emoteChange
    if (emoteChange != null) {
        ChatEmoteChangeLine(
            message = message,
            change = emoteChange,
            readableColors = readableColors,
            stripeColor = highlightColor,
            showTimestamps = showTimestamps,
            textSize = textSize,
            timestampSize = timestampSize,
            lineHeight = lineHeight,
            emoteSize = emoteSize,
            logo = appearance.platformLogos[emoteChange.platform],
        )
        return
    }
    val displayMessage = remember(message, appearance.thirdPartyEmotes, appearance.modifierPlatforms) {
        message.withDisplayParts(appearance.thirdPartyEmotes, appearance.modifierPlatforms)
    }
    val channelAccent = displayMessage.color.takeIf { it != Color.Unspecified } ?: TwitchPurple
    val accent = if (message.eventKind == ChatEventKind.Announcement &&
        displayMessage.accentColor == TwitchTextSecondary
    ) {
        channelAccent
    } else {
        displayMessage.accentColor ?: TwitchPurple
    }
    val eventColor = when (message.eventKind) {
        ChatEventKind.Subscription -> TwitchText
        ChatEventKind.System -> TwitchTextSecondary
        else -> accent
    }
    val rowBackground = when {
        mentioned -> ChatMention
        message.eventKind == ChatEventKind.Highlight -> highlightColor.copy(alpha = 0.22f)
        message.firstInChannel -> ChatFirstTimeChatter
        highlightFirstMessages && message.firstInSession -> ChatFirstMessage
        else -> Color.Transparent
    }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var rowHeightPx by remember { mutableIntStateOf(0) }
    KeepSelectedMessageVisible(
        selected = selected,
        messageId = message.id,
        rowHeightPx = rowHeightPx,
        bottomOverlayPx = bottomOverlayPx,
        listState = listState,
        autoScrolling = autoScrolling,
    )
    val copyText = remember(message) { message.copyableText() }
    val canReply = onReply != null && chatMessageCanBeReplied(message)
    val replySwipe = canReply && !LocalPlayerContentFullscreen.current
    val timestampText = remember(message.timestampMillis, showTimestamps) {
        if (showTimestamps) formatChatTimestamp(context, message.timestampMillis) else null
    }
    val showUserMessage = message.parts.isNotEmpty() ||
        message.eventKind == ChatEventKind.Announcement ||
        message.eventKind == ChatEventKind.Highlight ||
        message.eventKind == ChatEventKind.Normal
    ChatReplySwipe(
        enabled = replySwipe,
        onReply = { onReply?.invoke(message) },
        onSwipingChange = onReplySwipingChange,
    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(
                if (message.eventKind == ChatEventKind.Reward ||
                    message.eventKind == ChatEventKind.Subscription ||
                    message.eventKind == ChatEventKind.WatchStreak ||
                    message.eventKind == ChatEventKind.Announcement ||
                    message.cheerBits != null
                ) {
                    Modifier.drawBehind {
                        val stripe = 3.dp.toPx()
                        val x = if (layoutDirection == LayoutDirection.Rtl) {
                            size.width - stripe
                        } else {
                            0f
                        }
                        drawRect(
                            color = when {
                                message.cheerBits != null -> channelColor
                                message.eventKind == ChatEventKind.Announcement -> accent
                                else -> highlightColor
                            },
                            topLeft = Offset(x, 0f),
                            size = Size(stripe, size.height),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .background(animatedSelectionOverlay(selected, CHAT_SELECTED_OVERLAY_ALPHA))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClickLabel = stringResource(
                    if (canReply) R.string.chat_message_actions else R.string.chat_copy,
                ),
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect()
                },
                onClick = { if (selected) onDismissSelection() },
            ),
    ) {
        val deletedAppearance = deletedMessageAppearance(displayMessage.deleted)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
                .onSizeChanged { rowHeightPx = it.height },
        ) {
        Column(modifier = Modifier.alpha(animatedContentAlpha(deletedAppearance.contentAlpha))) {
        if (message.firstInChannel) {
            Row(
                modifier = Modifier.padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_first_time_chatter),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(chatDp(chatTextSize, 14, 16)),
                )
                Text(
                    text = stringResource(R.string.chat_first_time_chatter),
                    color = Color.White,
                    fontSize = timestampSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        message.cheerBits?.let { bits ->
            Text(
                text = pluralStringResource(R.plurals.chat_cheered_bits, bits, bits),
                color = TwitchTextSecondary,
                fontSize = timestampSize,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (message.eventKind == ChatEventKind.Announcement) {
            ChatAnnouncementHeader(
                accent = accent,
                chatTextSize = chatTextSize,
                timestampSize = timestampSize,
                lineHeight = lineHeight,
            )
        }
        if (message.eventKind == ChatEventKind.Highlight ||
            message.eventKind == ChatEventKind.Reward
        ) {
            val rewardTitle = if (message.eventKind == ChatEventKind.Highlight) {
                stringResource(R.string.chat_highlight_reward_title)
            } else {
                message.reward?.title?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.chat_reward_unnamed)
            }
            ChatRewardRedeemedHeader(
                message = message,
                title = rewardTitle,
                cost = if (message.eventKind == ChatEventKind.Highlight) {
                    highlightRewardCost
                } else {
                    message.reward?.cost ?: 0
                },
                hasUserInput = message.eventKind == ChatEventKind.Highlight ||
                    message.parts.isNotEmpty(),
                timestampText = timestampText,
                pointsIconUrl = channelPointsIconUrl,
                chatTextSize = chatTextSize,
                textSize = textSize,
                timestampSize = timestampSize,
                lineHeight = lineHeight,
            )
        }
        if (message.raid != null) {
            ChatRaidLine(
                raid = message.raid,
                timestampText = timestampText,
                textSize = textSize,
                timestampSize = timestampSize,
                lineHeight = lineHeight,
            )
        } else if (message.watchStreak != null) {
            ChatWatchStreakLine(
                message = message,
                streak = message.watchStreak,
                timestampText = timestampText.takeIf {
                    !watchStreakTimestampOnUserMessage(message.parts.isNotEmpty())
                },
                pointsIconUrl = channelPointsIconUrl,
                chatTextSize = chatTextSize,
                textSize = textSize,
                timestampSize = timestampSize,
                lineHeight = lineHeight,
            )
        } else if (message.eventKind == ChatEventKind.Subscription &&
            !message.systemText.isNullOrBlank()
        ) {
            val nickColor = remember(message.color, readableColors) {
                if (readableColors) ReadableChatColor.adjust(message.color, TwitchBg) else message.color
            }
            ChatSubscriptionLine(
                message = message,
                text = message.systemText,
                timestampText = timestampText,
                nickColor = nickColor,
                chatTextSize = chatTextSize,
                textSize = textSize,
                timestampSize = timestampSize,
                lineHeight = lineHeight,
            )
        } else if (message.moderationNotice != null) {
            ChatModerationNoticeLine(message.moderationNotice, timestampText, textSize, lineHeight)
        } else if (message.warningNotice != null) {
            ChatWarningNoticeLine(message.warningNotice, timestampText, textSize, lineHeight)
        } else if (!message.systemText.isNullOrBlank()) {
            Text(
                text = buildString {
                    if (timestampText != null) {
                        append(timestampText)
                        append(" ")
                    }
                    append(message.systemText)
                },
                color = eventColor,
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                lineHeight = lineHeight,
            )
        }
        if (showUserMessage && message.displayName.isNotEmpty()) {
            ChatModerationFrame(
                message = message,
                iconSize = chatDp(chatTextSize, 14, 16),
                textSize = timestampSize,
            ) {
                message.reply?.let { reply ->
                    ChatReplyPreview(
                        reply = reply,
                        chatTextSize = chatTextSize,
                        meMessageItalic = meMessageItalic,
                        expanded = selected,
                    )
                }
                ChatUserMessage(
                    message = displayMessage,
                    readableColors = readableColors,
                    timestampText = timestampText.takeIf {
                        if (message.watchStreak != null) {
                            watchStreakTimestampOnUserMessage(message.parts.isNotEmpty())
                        } else {
                            message.systemText.isNullOrBlank()
                        }
                    },
                    textSize = textSize,
                    timestampSize = timestampSize,
                    lineHeight = lineHeight,
                    emoteSize = emoteSize,
                    gifSize = gifSize,
                    badgeSize = badgeSize,
                    badgeUrls = appearance.badgeUrls,
                    meMessageItalic = meMessageItalic,
                    onEmoteLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect()
                    },
                )
            }
        }
        }
        ChatRowLinkPreview(message.rawText, linkPreviewMode, deletedAppearance.linkPreviewAlpha)
        if (selected && (copyText != null || canReply)) {
            ChatMessageCopyPopup(
                onReply = if (canReply) {
                    {
                        onReply(message)
                        onDismissSelection()
                    }
                } else {
                    null
                },
                onCopy = copyText?.let { text ->
                    {
                        context.copyChatText(text)
                        onDismissSelection()
                    }
                },
                onDismiss = onDismissSelection,
            )
        }
    }
}
}
}

