package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelHypeTrain
import name.alexwayfer.customtv.chat.HypeTrainPhase
import name.alexwayfer.customtv.chat.hypeTrainPercent
import name.alexwayfer.customtv.chat.twitchEmoteUrl
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.rememberLastNonNull
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
    // The same scale as the other notices: the name and the timer read like a title, the level and share like a label.
    val titleSize = chatSp(chatTextSize, 14, 16)
    val labelSize = chatSp(chatTextSize, 12, 14)
    val color = hypeTrainColor(train.colorHex) ?: TwitchPurple
    val percent = hypeTrainPercent(train)
    val completedLevel = rememberCompletedLevel(train)
    val fill by animateFloatAsState(
        targetValue = when {
            completedLevel != null -> 1f
            train.phase == HypeTrainPhase.Approaching -> 0f
            else -> percent / 100f
        },
        animationSpec = tween(VOTE_ANIMATION_MS),
        label = "hype train fill",
    )
    val timer = if (train.phase == HypeTrainPhase.Ended) null else rememberCountdownLabel(train.endsAtMillis)
    val label = stringResource(
        when (train.phase) {
            HypeTrainPhase.Approaching -> R.string.chat_hype_train_approaching
            HypeTrainPhase.Active -> R.string.chat_hype_train
            HypeTrainPhase.Ended -> R.string.chat_hype_train_ended
        },
    )
    val value = if (train.phase == HypeTrainPhase.Approaching) {
        pluralStringResource(R.plurals.chat_hype_train_events_to_start, train.eventsToStart, train.eventsToStart)
    } else {
        stringResource(R.string.chat_hype_train_level, train.level)
    }
    val summary = if (train.phase == HypeTrainPhase.Approaching) {
        "$label, $value"
    } else {
        stringResource(R.string.chat_hype_train_description, label, train.level, percent)
    }
    val description = timer?.let { "$summary, ${stringResource(R.string.chat_hype_train_time_left, it)}" } ?: summary
    val completedText = completedLevel?.let { stringResource(R.string.chat_hype_train_level_completed, it) }
    val expandLabel = stringResource(if (expanded) R.string.chat_vote_collapse else R.string.chat_vote_expand)
    // The completed level fades in over the level and timer, and they fade back once it is over.
    val celebration by animateFloatAsState(if (completedText != null) 1f else 0f, label = "hype train celebration")
    val shownCompletedText = rememberLastNonNull(completedText)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(color, size = Size(size.width * fill, size.height)) }
                .clickable { onExpandedChange(!expanded) }
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = completedText ?: description },
                contentAlignment = Alignment.Center,
            ) {
                // The level and timer stay laid out under the celebration, so the card keeps its height.
                Row(
                    modifier = Modifier.graphicsLayer { alpha = 1f - celebration },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Bold)
                        Text(value, color = Color.White, fontSize = labelSize, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        timer?.let { Text(it, color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Bold) }
                        if (train.phase != HypeTrainPhase.Approaching) {
                            Text("$percent%", color = Color.White, fontSize = labelSize)
                        }
                    }
                }
                if (completedText != null || celebration > 0f) {
                    shownCompletedText?.let {
                        Text(
                            it,
                            color = Color.White,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer { alpha = celebration },
                        )
                    }
                }
            }
            NoticeExpandArrow(
                expanded = expanded,
                contentDescription = expandLabel,
                tint = Color.White,
                modifier = Modifier.padding(start = 6.dp).size(chatDp(chatTextSize, 20, 22)),
            )
        }
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

/**
 * The level the train just completed, for [LEVEL_COMPLETED_SHOWN] after it moves up; null
 * otherwise. The first frame and a new train show no celebration.
 */
@Composable
private fun rememberCompletedLevel(train: ChannelHypeTrain): Int? {
    var shownLevel by remember(train.id) { mutableIntStateOf(train.level) }
    var completed by remember(train.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(train.id, train.level) {
        val level = completedHypeTrainLevel(shownLevel, train.level)
        shownLevel = train.level
        if (level != null) {
            completed = level
            delay(LEVEL_COMPLETED_SHOWN)
            completed = null
        }
    }
    return completed
}

/** The level just completed when the train moves from [previous] to [current]; a jump of several names the last. */
internal fun completedHypeTrainLevel(previous: Int, current: Int): Int? =
    (current - 1).takeIf { previous in 1..<current }

private val LEVEL_COMPLETED_SHOWN = 5.seconds

/** The time left until [endsAtMillis] as "m:ss", ticking each second. */
@Composable
private fun rememberCountdownLabel(endsAtMillis: Long): String {
    var now by remember(endsAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtMillis) {
        while (now < endsAtMillis) {
            delay(1.seconds)
            now = System.currentTimeMillis()
        }
    }
    return countdownLabel(endsAtMillis - now)
}

/** "m:ss" for the time left, rounded up so the last second shows 0:01 rather than 0:00. */
internal fun countdownLabel(millisLeft: Long): String {
    val seconds = ((millisLeft.coerceAtLeast(0L) + 999L) / 1_000L)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private val APPROACH_STRIPE_WIDTH = 4.dp

/** The train's color from Twitch's "RRGGBB"; null when the streamer set none. */
internal fun hypeTrainColor(hex: String?): Color? {
    val value = hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}
