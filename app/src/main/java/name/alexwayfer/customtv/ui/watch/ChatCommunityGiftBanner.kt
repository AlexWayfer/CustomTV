package name.alexwayfer.customtv.ui.watch

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatMessage
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatCommunityGiftBanner(
    message: ChatMessage,
    chatTextSize: Int,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    val gift = message.communityGift ?: return
    val name = gift.gifterDisplayName ?: stringResource(R.string.chat_community_gift_anonymous)
    val action = if (gift.tier == null) {
        pluralStringResource(
            R.plurals.chat_community_gift_subscriptions,
            gift.count,
            gift.count,
        )
    } else {
        pluralStringResource(
            R.plurals.chat_community_gift_tier_subscriptions,
            gift.count,
            gift.count,
            gift.tier,
        )
    }
    val cumulative = gift.cumulativeCount
        ?.takeIf { it >= gift.count }
        ?.let {
            pluralStringResource(R.plurals.chat_community_gift_total, it, it)
        }
    val headerSize = chatSp(chatTextSize, 12, 14)
    val bodySize = chatSp(chatTextSize, 14, 16)
    val hideLabel = stringResource(R.string.chat_community_gift_hide)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .blockChatTouches(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = TwitchPurple,
                modifier = Modifier.size(chatDp(chatTextSize, 16, 18)),
            )
            Text(
                text = stringResource(R.string.chat_community_gift),
                color = TwitchTextSecondary,
                fontSize = headerSize,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = hideLabel,
                tint = TwitchTextSecondary,
                modifier = Modifier
                    .size(chatDp(chatTextSize, 28, 32))
                    .clickable(onClick = onHide)
                    .padding(6.dp),
            )
        }
        Text(
            text = name,
            color = TwitchText,
            fontWeight = FontWeight.Bold,
            fontSize = bodySize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp),
        )
        Text(
            text = action,
            color = TwitchText,
            fontSize = bodySize,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp),
        )
        if (cumulative != null) {
            Text(
                text = cumulative,
                color = TwitchTextSecondary,
                fontSize = headerSize,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
            )
        }
        Spacer(Modifier.height(bottomInset + noticeGapAboveInset(10.dp, bottomInset)))
    }
}
