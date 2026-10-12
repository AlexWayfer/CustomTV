package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.secondsRoundedUp
import name.alexwayfer.customtv.chat.ChannelHypeTrain
import name.alexwayfer.customtv.chat.HypeTrainPhase
import name.alexwayfer.customtv.chat.opaqueArgbFromHex
import name.alexwayfer.customtv.chat.twitchEmoteUrl
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText

/**
 * The hype train notice, shaped like Twitch's own: the train's color fills the card as the level
 * fills, with the level on the left and the time left and share on the right. A tap shows the
 * conductors and the level's emote rewards.
 */
@Composable
internal fun ChatHypeTrainBanner(
    train: ChannelHypeTrain,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    val labelSize = chatSp(chatTextSize, 12, 14)
    val color = hypeTrainColor(train.colorHex) ?: TwitchPurple
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = ChatNoticeCardOuterPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(TwitchSurface)
            .drawWithContent {
                drawContent()
                // An approach has no fill yet; Twitch marks it with a stripe of the creator color instead.
                if (train.phase == HypeTrainPhase.Approaching) {
                    drawRect(color, size = Size(APPROACH_STRIPE_WIDTH.toPx(), size.height))
                }
            }
            .blockChatTouches(),
    ) {
        HypeTrainHeader(train, color, expanded, onExpandedChange, chatTextSize)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            // New conductors and rewards open the details up while they are shown.
            Column(Modifier.animateContentSize()) {
                HypeTrainDetails(train, chatTextSize)
                ChatNoticeHideButton(
                    onClick = onHide,
                    textSize = labelSize,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                )
            }
        }
        if (bottomInset > 0.dp) {
            Spacer(Modifier.height(bottomInset))
        }
    }
}

@Composable
private fun HypeTrainDetails(train: ChannelHypeTrain, chatTextSize: Int) {
    val textSize = chatSp(chatTextSize, 14, 16)
    val emoteSize = chatDp(chatTextSize, 28, 32)
    Column(
        modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        train.conductors.forEach { conductor ->
            val role = stringResource(
                when (conductor.source) {
                    "SUBS" -> R.string.chat_hype_train_conductor_subs
                    "BITS" -> R.string.chat_hype_train_conductor_bits
                    else -> R.string.chat_hype_train_conductor
                },
            )
            Text(
                text = "$role: ${conductor.displayName}",
                color = TwitchText,
                fontSize = textSize,
            )
        }
        if (train.rewards.isNotEmpty()) {
            Text(
                text = stringResource(R.string.chat_hype_train_rewards, train.level.coerceAtLeast(1)),
                color = TwitchText,
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                train.rewards.forEach { emote ->
                    SharedEmoteImage(
                        url = twitchEmoteUrl(emote.id),
                        contentDescription = emote.token.ifBlank { null },
                        size = emoteSize,
                    )
                }
            }
        }
    }
}

/** The level just completed when the train moves from [previous] to [current]; a jump of several names the last. */
internal fun completedHypeTrainLevel(previous: Int, current: Int): Int? =
    (current - 1).takeIf { previous in 1..<current }

/** "m:ss" for the time left, rounded up so the last second shows 0:01 rather than 0:00. */
internal fun countdownLabel(millisLeft: Long): String =
    secondsRoundedUp(millisLeft.coerceAtLeast(0L)).seconds.toComponents { minutes, rest, _ ->
        "%d:%02d".format(Locale.ROOT, minutes, rest)
    }

private val APPROACH_STRIPE_WIDTH = 4.dp

/** The train's color from Twitch's "RRGGBB"; null when the streamer set none. */
internal fun hypeTrainColor(hex: String?): Color? = opaqueArgbFromHex(hex)?.let { Color(it) }
