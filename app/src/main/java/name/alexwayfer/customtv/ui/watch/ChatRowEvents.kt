package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatEventKind
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** What stands above the message: a first-time chatter, a cheer, an announcement, or a redeemed reward. */
@Composable
internal fun ChatRowHeaders(message: ChatMessage, look: ChatRowLook) {
    val sizes = look.sizes
    if (message.firstInChannel) {
        Row(
            modifier = Modifier.padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat_first_time_chatter),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(chatDp(sizes.chatTextSize, normal = 14, large = 16)),
            )
            Text(
                text = stringResource(R.string.chat_first_time_chatter),
                color = Color.White,
                fontSize = sizes.timestamp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
    message.cheerBits?.let { bits ->
        Text(
            text = pluralStringResource(R.plurals.chat_cheered_bits, bits, bits),
            color = TwitchTextSecondary,
            fontSize = sizes.timestamp,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
    if (message.eventKind == ChatEventKind.Announcement) {
        ChatAnnouncementHeader(
            accent = look.accent,
            chatTextSize = sizes.chatTextSize,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
        )
    }
    if (message.eventKind == ChatEventKind.Highlight || message.eventKind == ChatEventKind.Reward) {
        ChatRowRewardHeader(message, look)
    }
}

@Composable
private fun ChatRowRewardHeader(message: ChatMessage, look: ChatRowLook) {
    val highlight = message.eventKind == ChatEventKind.Highlight
    val rewardTitle = if (highlight) {
        stringResource(R.string.chat_highlight_reward_title)
    } else {
        message.reward?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_reward_unnamed)
    }
    val sizes = look.sizes
    ChatRewardRedeemedHeader(
        message = message,
        title = rewardTitle,
        cost = if (highlight) look.highlightRewardCost else message.reward?.cost ?: 0,
        hasUserInput = highlight || message.parts.isNotEmpty(),
        timestampText = look.timestampText,
        pointsIconUrl = look.pointsIconUrl,
        chatTextSize = sizes.chatTextSize,
        textSize = sizes.text,
        timestampSize = sizes.timestamp,
        lineHeight = sizes.lineHeight,
    )
}

/** The event the row tells of: a raid, a watch streak, a subscription, a moderation notice, or a system line. */
@Composable
internal fun ChatRowEventLine(message: ChatMessage, look: ChatRowLook) {
    val sizes = look.sizes
    val timestampText = look.timestampText
    when {
        message.raid != null -> ChatRaidLine(
            raid = message.raid,
            timestampText = timestampText,
            textSize = sizes.text,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
        )
        message.watchStreak != null -> ChatWatchStreakLine(
            message = message,
            streak = message.watchStreak,
            timestampText = timestampText.takeIf { !watchStreakTimestampOnUserMessage(message.parts.isNotEmpty()) },
            pointsIconUrl = look.pointsIconUrl,
            chatTextSize = sizes.chatTextSize,
            textSize = sizes.text,
            timestampSize = sizes.timestamp,
            lineHeight = sizes.lineHeight,
        )
        message.eventKind == ChatEventKind.Subscription && !message.systemText.isNullOrBlank() -> {
            val readableColors = look.appearance.readableColors
            val nickColor = remember(message.color, readableColors) {
                if (readableColors) ReadableChatColor.adjust(message.color, TwitchBg) else message.color
            }
            ChatSubscriptionLine(
                message = message,
                text = message.systemText,
                timestampText = timestampText,
                nickColor = nickColor,
                chatTextSize = sizes.chatTextSize,
                badgeSize = sizes.badge,
                textSize = sizes.text,
                timestampSize = sizes.timestamp,
                lineHeight = sizes.lineHeight,
            )
        }
        message.moderationNotice != null ->
            ChatModerationNoticeLine(message.moderationNotice, timestampText, sizes.text, sizes.lineHeight)
        message.warningNotice != null ->
            ChatWarningNoticeLine(message.warningNotice, timestampText, sizes.text, sizes.lineHeight)
        !message.systemText.isNullOrBlank() -> ChatSystemTextLine(message, message.systemText, look)
    }
}

/** A notice's own line; one shared from another channel starts with that channel's avatar. */
@Composable
private fun ChatSystemTextLine(message: ChatMessage, text: String, look: ChatRowLook) {
    val sizes = look.sizes
    val source = sharedChatSourceLook(message.sourceRoomId)
    val sourceSizes = chatTextSizes(sizes.text, sizes.timestamp, sizes.emote, sizes.badge)
    val inlineContent = remember(message) { linkedMapOf<String, InlineTextContent>() }
    val annotated = remember(text, look.timestampText, source, sourceSizes, inlineContent) {
        inlineContent.clear()
        buildAnnotatedString {
            look.timestampText?.let { timestamp ->
                append(timestamp)
                append(" ")
            }
            appendSharedChatSource(source, sourceSizes, inlineContent)
            append(text)
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        color = when (message.eventKind) {
            ChatEventKind.Subscription -> TwitchText
            ChatEventKind.System -> TwitchTextSecondary
            else -> look.accent
        },
        fontSize = sizes.text,
        fontWeight = FontWeight.Medium,
        lineHeight = sizes.lineHeight,
    )
}

/** What the parts of a chat row draw with: the chat's look, the row's sizes and timestamp, and its colors. */
internal data class ChatRowLook(
    val appearance: ChatAppearance,
    val sizes: ChatRowSizes,
    val timestampText: String?,
    val accent: Color,
    val pointsIconUrl: String?,
    val highlightRewardCost: Int,
)
