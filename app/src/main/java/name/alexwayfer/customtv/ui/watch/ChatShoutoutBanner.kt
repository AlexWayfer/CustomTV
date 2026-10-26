package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelShoutout
import name.alexwayfer.customtv.data.ChannelFollowStatus
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.data.showsFollowButton
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/**
 * The plaque of a shoutout: the channel's avatar and name under who gave it, which open the channel's card, its
 * heart, and the hide button.
 */
@Composable
internal fun ChatShoutoutBanner(
    shoutout: ChannelShoutout,
    sourceName: String?,
    follow: ShoutoutFollow,
    chatTextSize: Int,
    onOpenCard: () -> Unit,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .blockChatTouches(),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShoutoutChannel(
                shoutout = shoutout,
                sourceName = sourceName,
                chatTextSize = chatTextSize,
                onOpenCard = onOpenCard,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(visible = follow.status.showsFollowButton) {
                FollowButton(follow.status == ChannelFollowStatus.Following, follow.action)
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.chat_shoutout_hide),
                tint = TwitchTextSecondary,
                modifier = Modifier
                    .size(chatDp(chatTextSize, normal = 28, large = 32))
                    .clickable(onClick = onHide)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(bottomInset + noticeGapAboveInset(6.dp, bottomInset)))
    }
}

/** The shouted out channel's avatar and name under who gave the shoutout, read as one line that opens the card. */
@Composable
private fun ShoutoutChannel(
    shoutout: ChannelShoutout,
    sourceName: String?,
    chatTextSize: Int,
    onOpenCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (sourceName == null) {
        stringResource(R.string.chat_shoutout_description_anonymous, shoutout.targetDisplayName)
    } else {
        stringResource(R.string.chat_shoutout_description, sourceName, shoutout.targetDisplayName)
    }
    val headerSize = chatSp(chatTextSize, 12, 14)
    val nameSize = chatSp(chatTextSize, 14, 16)
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onOpenCard),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clearAndSetSemantics {}) {
            ChannelAvatar(
                channel = shoutout.targetLogin,
                profile = ChannelProfile(
                    login = shoutout.targetLogin,
                    displayName = shoutout.targetDisplayName,
                    avatarUrl = shoutout.targetAvatarUrl,
                ),
                size = chatDp(chatTextSize, normal = 32, large = 36),
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .clearAndSetSemantics {},
        ) {
            Text(
                text = sourceName?.let { stringResource(R.string.chat_shoutout_by, it) }
                    ?: stringResource(R.string.chat_shoutout),
                color = TwitchTextSecondary,
                fontSize = headerSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = shoutout.targetDisplayName,
                color = TwitchText,
                fontWeight = FontWeight.Bold,
                fontSize = nameSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
