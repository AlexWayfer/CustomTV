package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.OutgoingRaid
import name.alexwayfer.customtv.chat.raidFractionRemaining
import name.alexwayfer.customtv.chat.raidSecondsRemaining
import name.alexwayfer.customtv.data.ChannelProfile
import name.alexwayfer.customtv.ui.components.ChannelAvatar
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatRaidNotice(
    raid: OutgoingRaid,
    chatTextSize: Int,
    dotInset: Dp,
) {
    var seconds by remember(raid.id, raid.goAtMillis) {
        mutableIntStateOf(raidSecondsRemaining(raid.goAtMillis, System.currentTimeMillis()))
    }
    val durationMillis = remember(raid.id, raid.goAtMillis) {
        (raid.goAtMillis - System.currentTimeMillis()).coerceAtLeast(1L)
    }
    var fraction by remember(raid.id, raid.goAtMillis) {
        mutableFloatStateOf(
            raidFractionRemaining(raid.goAtMillis, System.currentTimeMillis(), durationMillis),
        )
    }
    LaunchedEffect(raid.id, raid.goAtMillis) {
        while (isActive) {
            val now = System.currentTimeMillis()
            seconds = raidSecondsRemaining(raid.goAtMillis, now)
            fraction = raidFractionRemaining(raid.goAtMillis, now, durationMillis)
            if (seconds == 0) return@LaunchedEffect
            withFrameMillis { }
        }
    }
    val headerSize = chatSp(chatTextSize, 12, 14)
    val bodySize = chatSp(chatTextSize, 14, 16)
    val timerSize = chatDp(chatTextSize, 36, 40)
    val viewerCount = pluralStringResource(
        R.plurals.raid_outgoing_viewer_count,
        raid.viewerCount,
        raid.viewerCount,
    )
    val viewers = pluralStringResource(
        R.plurals.raid_outgoing_with_viewers,
        raid.viewerCount,
        raid.targetDisplayName,
        raid.viewerCount,
    )
    val description = pluralStringResource(R.plurals.raid_outgoing_countdown, seconds, viewers, seconds)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .blockChatTouches()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            content = {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chat_megaphone),
                            contentDescription = null,
                            tint = TwitchTextSecondary,
                            modifier = Modifier.size(chatDp(chatTextSize, 16, 18)),
                        )
                        Text(
                            text = stringResource(R.string.chat_raid_notice),
                            color = TwitchTextSecondary,
                            fontSize = headerSize,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.raid_outgoing_lead),
                            color = TwitchText,
                            fontSize = bodySize,
                            maxLines = 1,
                        )
                        if (raid.targetAvatarUrl != null) {
                            ChannelAvatar(
                                channel = raid.targetLogin,
                                profile = ChannelProfile(
                                    login = raid.targetLogin,
                                    displayName = raid.targetDisplayName,
                                    avatarUrl = raid.targetAvatarUrl,
                                ),
                                size = chatDp(chatTextSize, 18, 22),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Text(
                            text = raid.targetDisplayName,
                            color = TwitchText,
                            fontWeight = FontWeight.Bold,
                            fontSize = bodySize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .weight(1f, fill = false),
                        )
                        Text(
                            text = stringResource(R.string.raid_outgoing_with),
                            color = TwitchText,
                            fontSize = bodySize,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            text = viewerCount,
                            color = TwitchText,
                            fontWeight = FontWeight.Bold,
                            fontSize = bodySize,
                            maxLines = 1,
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(timerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(timerSize),
                        color = TwitchPurple,
                        trackColor = TwitchDivider,
                        strokeWidth = 2.dp,
                        gapSize = 0.dp,
                    )
                    Text(
                        text = seconds.toString(),
                        color = TwitchText,
                        fontWeight = FontWeight.Bold,
                        fontSize = bodySize,
                    )
                }
            },
        ) { measurables, constraints ->
            val timerPx = timerSize.roundToPx()
            val gapPx = 8.dp.roundToPx()
            val textWidth = (constraints.maxWidth - timerPx - gapPx).coerceAtLeast(0)
            val text = measurables[0].measure(
                constraints.copy(minWidth = 0, minHeight = 0, maxWidth = textWidth),
            )
            val timer = measurables[1].measure(Constraints.fixed(timerPx, timerPx))
            val height = maxOf(text.height, timer.height)
            layout(constraints.maxWidth, height) {
                text.placeRelative(0, 0)
                timer.placeRelative(constraints.maxWidth - timerPx, 0)
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = TwitchPurple,
            trackColor = TwitchDivider,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        if (dotInset > 0.dp) {
            Spacer(Modifier.height(dotInset))
        }
    }
}
