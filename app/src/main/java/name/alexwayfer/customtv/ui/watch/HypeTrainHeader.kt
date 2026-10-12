package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelHypeTrain
import name.alexwayfer.customtv.chat.HypeTrainPhase
import name.alexwayfer.customtv.chat.hypeTrainPercent
import name.alexwayfer.customtv.ui.components.rememberLastNonNull

/**
 * The hype train card's top: the train's [color] fills it as the level fills, with the level on the left and the
 * time left and share on the right. A completed level shows over them for a moment; a tap opens the details.
 */
@Composable
internal fun HypeTrainHeader(
    train: ChannelHypeTrain,
    color: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
) {
    val titleSize = chatSp(chatTextSize, 14, 16)
    val completedLevel = rememberCompletedLevel(train)
    val fill by animateFloatAsState(
        targetValue = when {
            completedLevel != null -> 1f
            train.phase == HypeTrainPhase.Approaching -> 0f
            else -> hypeTrainPercent(train) / 100f
        },
        animationSpec = tween(VOTE_ANIMATION_MS),
        label = "hype train fill",
    )
    val timer = if (train.phase == HypeTrainPhase.Ended) null else rememberCountdownLabel(train.endsAtMillis)
    val description = hypeTrainDescription(train, timer)
    val completedText = completedLevel?.let { stringResource(R.string.chat_hype_train_level_completed, it) }
    val expandLabel = stringResource(if (expanded) R.string.chat_vote_collapse else R.string.chat_vote_expand)
    // The completed level fades in over the level and timer, and they fade back once it is over.
    val celebration by animateFloatAsState(if (completedText != null) 1f else 0f, label = "hype train celebration")
    val shownCompletedText = rememberLastNonNull(completedText)
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
            HypeTrainLevelAndTimer(train, timer, chatTextSize, contentAlpha = { 1f - celebration })
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
            modifier = Modifier.padding(start = 6.dp).size(chatDp(chatTextSize, normal = 20, large = 22)),
        )
    }
}

/** The level, or the events still needed to start, on the left; the time left and the share on the right. */
@Composable
private fun HypeTrainLevelAndTimer(
    train: ChannelHypeTrain,
    timer: String?,
    chatTextSize: Int,
    contentAlpha: () -> Float,
) {
    // The same scale as the other notices: the name and the timer read like a title, the level and share like a label.
    val titleSize = chatSp(chatTextSize, 14, 16)
    val labelSize = chatSp(chatTextSize, 12, 14)
    val label = hypeTrainLabel(train.phase)
    val value = hypeTrainValue(train)
    Row(
        modifier = Modifier.graphicsLayer { alpha = contentAlpha() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = labelSize, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            timer?.let { Text(it, color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Bold) }
            if (train.phase != HypeTrainPhase.Approaching) {
                Text("${hypeTrainPercent(train)}%", color = Color.White, fontSize = labelSize)
            }
        }
    }
}

/** What TalkBack reads for the card's top: the phase, the level and share or the events to start, the time left. */
@Composable
private fun hypeTrainDescription(train: ChannelHypeTrain, timer: String?): String {
    val label = hypeTrainLabel(train.phase)
    val summary = if (train.phase == HypeTrainPhase.Approaching) {
        "$label, ${hypeTrainValue(train)}"
    } else {
        stringResource(R.string.chat_hype_train_description, label, train.level, hypeTrainPercent(train))
    }
    return timer?.let { "$summary, ${stringResource(R.string.chat_hype_train_time_left, it)}" } ?: summary
}

@Composable
private fun hypeTrainLabel(phase: HypeTrainPhase): String = stringResource(
    when (phase) {
        HypeTrainPhase.Approaching -> R.string.chat_hype_train_approaching
        HypeTrainPhase.Active -> R.string.chat_hype_train
        HypeTrainPhase.Ended -> R.string.chat_hype_train_ended
    },
)

@Composable
private fun hypeTrainValue(train: ChannelHypeTrain): String = if (train.phase == HypeTrainPhase.Approaching) {
    pluralStringResource(R.plurals.chat_hype_train_events_to_start, train.eventsToStart, train.eventsToStart)
} else {
    stringResource(R.string.chat_hype_train_level, train.level)
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
