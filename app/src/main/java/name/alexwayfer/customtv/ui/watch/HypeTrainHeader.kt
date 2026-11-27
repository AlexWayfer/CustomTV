package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.drawOutline
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
import name.alexwayfer.customtv.ui.components.rememberLastNonNull

/**
 * The hype train card's top, with the level on the left and the time left and share on the right.
 * A completed level shows over them for a moment; a tap opens the details.
 */
@Composable
internal fun HypeTrainHeader(
    train: ChannelHypeTrain,
    completedLevel: Int?,
    color: Color,
    style: TrainBannerStyle?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
    bottomInset: Dp,
    fill: () -> Float,
    fold: () -> Float,
) {
    Column(
        Modifier.fillMaxWidth().drawBehind {
            val corner = 8.dp * fold().coerceIn(0f, 1f)
            val outline = RoundedCornerShape(bottomStart = corner, bottomEnd = corner).createOutline(
                size = if (style != null) size else Size(size.width * fill().coerceIn(0f, 1f), size.height),
                layoutDirection = layoutDirection,
                density = this,
            )
            if (style != null) drawOutline(outline, style.gradient) else drawOutline(outline, color)
        },
    ) {
        HypeTrainHeaderContent(train, style, completedLevel, expanded, onExpandedChange, chatTextSize, fill)
        Spacer(Modifier.height(bottomInset))
    }
}

@Composable
private fun HypeTrainHeaderContent(
    train: ChannelHypeTrain,
    style: TrainBannerStyle?,
    completedLevel: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
    fill: () -> Float,
) {
    val titleSize = chatSp(chatTextSize, 14, 16)
    val timer = if (train.phase == HypeTrainPhase.Ended) null else rememberCountdownLabel(train.endsAtMillis)
    val label = hypeTrainLabel(train.phase, style?.title)
    val description = hypeTrainDescription(train, timer, label)
    val completedText = completedLevel?.let { stringResource(R.string.chat_hype_train_level_completed, it) }
    val expandLabel = stringResource(if (expanded) R.string.chat_vote_collapse else R.string.chat_vote_expand)
    // The completed level fades in over the level and timer, and they fade back once it is over.
    val celebration by animateFloatAsState(if (completedText != null) 1f else 0f, label = "hype train celebration")
    val shownCompletedText = rememberLastNonNull(completedText)
    ChatNoticeHeader(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        chatTextSize = chatTextSize,
        expandLabel = expandLabel,
        arrowTint = Color.White,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
                .clearAndSetSemantics { contentDescription = completedText ?: description },
            contentAlignment = Alignment.Center,
        ) {
            // The level and timer stay laid out under the celebration, so the card keeps its height.
            HypeTrainLevelAndTimer(train, label, timer, chatTextSize, fill.takeIf { style != null }) {
                1f - celebration
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
    }
}

/** The level, or the events still needed to start, on the left; the time left and the share on the right. */
@Composable
private fun HypeTrainLevelAndTimer(
    train: ChannelHypeTrain,
    label: String,
    timer: String?,
    chatTextSize: Int,
    fill: (() -> Float)?,
    contentAlpha: () -> Float,
) {
    // The same scale as the other notices: the name and the timer read like a title, the level and share like a label.
    val titleSize = chatSp(chatTextSize, 14, 16)
    val labelSize = chatSp(chatTextSize, 12, 14)
    val value = hypeTrainValue(train)
    Row(
        modifier = Modifier.graphicsLayer { alpha = contentAlpha() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White, fontSize = labelSize, fontWeight = FontWeight.Bold)
                if (fill != null && train.phase == HypeTrainPhase.Active) {
                    LinearProgressIndicator(
                        progress = fill,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp).height(8.dp).clearAndSetSemantics { },
                        color = Color.White,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
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
private fun hypeTrainDescription(train: ChannelHypeTrain, timer: String?, label: String): String {
    val summary = if (train.phase == HypeTrainPhase.Approaching) {
        "$label, ${hypeTrainValue(train)}"
    } else {
        stringResource(R.string.chat_hype_train_description, label, train.level, hypeTrainPercent(train))
    }
    return timer?.let { "$summary, ${stringResource(R.string.chat_hype_train_time_left, it)}" } ?: summary
}

@Composable
private fun hypeTrainLabel(phase: HypeTrainPhase, name: String?): String {
    if (name != null) {
        return when (phase) {
            HypeTrainPhase.Approaching -> stringResource(R.string.chat_heist_train_approaching, name)
            HypeTrainPhase.Active -> name
            HypeTrainPhase.Ended -> stringResource(R.string.chat_heist_train_ended, name)
        }
    }
    val label = when (phase) {
        HypeTrainPhase.Approaching -> R.string.chat_hype_train_approaching
        HypeTrainPhase.Active -> R.string.chat_hype_train
        HypeTrainPhase.Ended -> R.string.chat_hype_train_ended
    }
    return stringResource(label)
}

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
internal fun rememberCompletedLevel(train: ChannelHypeTrain): Int? {
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
