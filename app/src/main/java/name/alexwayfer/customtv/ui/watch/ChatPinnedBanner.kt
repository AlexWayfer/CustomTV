package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.PinnedChat
import name.alexwayfer.customtv.chat.ReadableChatColor
import name.alexwayfer.customtv.chat.formatChatTimestamp
import name.alexwayfer.customtv.chat.withDisplayParts
import name.alexwayfer.customtv.ui.components.CardFoldSpring
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.theme.TwitchBg
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatPinnedBanner(
    pin: PinnedChat,
    channelLogin: String,
    appearance: ChatAppearance,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    val message = pin.message
    val readableColors = appearance.readableColors
    val chatTextSize = appearance.textSize
    val badgeUrls = appearance.badgeUrls
    val textSize = chatSp(chatTextSize, 13, 16)
    val timestampSize = chatSp(chatTextSize, 12, 14)
    val lineHeight = chatSp(chatTextSize, 21, 25)
    val emoteSize = chatDp(chatTextSize, 22, 26)
    val gifSize = chatDp(chatTextSize, 96, 112)
    val badgeSize = chatDp(chatTextSize, 14, 18)
    val headerSize = chatSp(chatTextSize, 12, 14)
    val displayMessage = remember(message, appearance.thirdPartyEmotes, appearance.modifierPlatforms) {
        message.withDisplayParts(appearance.thirdPartyEmotes, appearance.modifierPlatforms)
    }
    val context = LocalContext.current
    val sentAt = remember(message.timestampMillis) {
        formatChatTimestamp(context, message.timestampMillis)
    }
    val pinLabel = stringResource(R.string.chat_pinned)
    val expandLabel = stringResource(
        if (expanded) R.string.chat_pinned_collapse else R.string.chat_pinned_expand,
    )
    // The message keeps every line while the card folds, and the fold cuts them off from the bottom.
    val transition = updateTransition(expanded, label = "pinnedBanner")
    val fold by transition.animateFloat({ CardFoldSpring }, label = "pinnedFold") { if (it) 1f else 0f }
    val allLines = transition.currentState || transition.targetState
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .blockChatTouches()
            .semantics { contentDescription = pinLabel },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat_pin),
                contentDescription = null,
                tint = TwitchTextSecondary,
                modifier = Modifier.size(chatDp(chatTextSize, 16, 18)),
            )
            Text(
                text = stringResource(R.string.chat_pinned_by),
                color = TwitchTextSecondary,
                fontSize = headerSize,
                modifier = Modifier.padding(start = 8.dp, end = 6.dp),
            )
            ChatNameWithBadges(
                displayName = pin.pinnedBy?.displayName ?: message.displayName,
                color = TwitchText,
                badges = listOfNotNull(pin.pinnedBy?.pinRoleBadge(channelLogin)),
                badgeUrls = badgeUrls,
                textSize = headerSize,
                badgeSize = badgeSize,
                modifier = Modifier.weight(1f),
            )
            NoticeExpandArrow(
                expanded = expanded,
                contentDescription = expandLabel,
                tint = TwitchTextSecondary,
                modifier = Modifier.size(chatDp(chatTextSize, 20, 22)),
            )
        }
        ChatUserMessage(
            message = displayMessage,
            readableColors = readableColors,
            timestampText = null,
            textSize = textSize,
            timestampSize = timestampSize,
            lineHeight = lineHeight,
            emoteSize = emoteSize,
            gifSize = gifSize,
            badgeSize = badgeSize,
            badgeUrls = badgeUrls,
            meMessageItalic = appearance.meMessageItalic,
            includeAuthor = false,
            maxLines = collapsibleTextMaxLines(allLines),
            overflow = if (allLines) TextOverflow.Clip else TextOverflow.Ellipsis,
            showGifs = expanded,
            enableLinks = expanded,
            foldProgress = { fold },
            modifier = Modifier
                .padding(
                    start = 10.dp,
                    end = 12.dp,
                    bottom = lerp(noticeGapAboveInset(10.dp, bottomInset), 8.dp, fold),
                )
                .then(if (expanded) Modifier else Modifier.clickable { onExpandedChange(true) }),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val authorColor = remember(message.color, readableColors) {
                        if (readableColors) {
                            ReadableChatColor.adjust(message.color, TwitchBg)
                        } else {
                            message.color
                        }
                    }
                    ChatNameWithBadges(
                        displayName = message.displayName,
                        color = authorColor,
                        badges = message.badges,
                        badgeUrls = badgeUrls,
                        textSize = headerSize,
                        badgeSize = badgeSize,
                        modifier = Modifier.weight(1f),
                        labelsUserId = message.userId,
                    )
                    Text(
                        text = stringResource(R.string.chat_pinned_sent_at, sentAt),
                        color = TwitchTextSecondary,
                        fontSize = headerSize,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                ChatNoticeHideButton(
                    onClick = onHide,
                    textSize = headerSize,
                    modifier = Modifier.padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = noticeGapAboveInset(10.dp, bottomInset),
                    ),
                )
            }
        }
        if (bottomInset > 0.dp) {
            Spacer(Modifier.height(bottomInset))
        }
    }
}
