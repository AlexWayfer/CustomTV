package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.raidFractionRemaining
import name.alexwayfer.customtv.ui.components.CardFoldSpring
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import kotlin.math.roundToInt

/** The card both notices share: a tap on the header shows the choices, like the pinned message. */
@Composable
internal fun ChatVoteCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    title: String,
    chatTextSize: Int,
    countdown: VoteCountdown?,
    browserLabel: String?,
    note: String?,
    action: (@Composable () -> Unit)?,
    headerAction: (@Composable () -> Unit)?,
    onOpenTwitchChat: () -> Unit,
    onHide: () -> Unit,
    bottomInset: Dp,
    choices: @Composable ColumnScope.() -> Unit,
) {
    val headerSize = chatSp(chatTextSize, 12, 14)
    val expandLabel = stringResource(if (expanded) R.string.chat_vote_collapse else R.string.chat_vote_expand)
    // The title keeps every line while the card folds, and the fold cuts them off from the bottom.
    val transition = updateTransition(expanded, label = "voteBanner")
    val fold by transition.animateFloat({ CardFoldSpring }, label = "voteFold") { if (it) 1f else 0f }
    val allLines = transition.currentState || transition.targetState
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
                .clickable { onExpandedChange(!expanded) }
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoteCardTitle(label, title, chatTextSize, allLines, fold = { fold })
            headerAction?.invoke()
            NoticeExpandArrow(
                expanded = expanded,
                contentDescription = expandLabel,
                tint = TwitchTextSecondary,
                modifier = Modifier.size(chatDp(chatTextSize, normal = 20, large = 22)),
            )
        }
        // The body folds with the card, so the countdown bar under it rides the bottom edge.
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Column(
                    modifier = Modifier.padding(start = 10.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = choices,
                )
                browserLabel?.let { VoteBrowserButton(it, onOpenTwitchChat, headerSize) }
                action?.invoke()
                note?.let { VoteNote(it, chatTextSize) }
                ChatNoticeHideButton(
                    onClick = onHide,
                    textSize = headerSize,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                )
            }
        }
        countdown?.let { VoteCountdownBar(it) }
        if (bottomInset > 0.dp) {
            Spacer(Modifier.height(bottomInset))
        }
    }
}

internal class VoteCountdown(val endsAtMillis: Long, val durationMillis: Long)

/** The notice's kind over its question; a folded card cuts the question off below its first line. */
@Composable
private fun RowScope.VoteCardTitle(
    label: String,
    title: String,
    chatTextSize: Int,
    allLines: Boolean,
    fold: () -> Float,
) {
    val headerSize = chatSp(chatTextSize, 12, 14)
    val titleSize = chatSp(chatTextSize, 14, 16)
    var titleFirstLineBottomPx by remember { mutableIntStateOf(0) }
    Column(Modifier.weight(1f)) {
        Text(
            text = label,
            color = TwitchTextSecondary,
            fontSize = headerSize,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = title,
            color = TwitchText,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            maxLines = collapsibleTextMaxLines(allLines),
            overflow = if (allLines) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { layout -> titleFirstLineBottomPx = layout.getLineBottom(0).roundToInt() },
            modifier = Modifier.revealLines({ titleFirstLineBottomPx }, fold),
        )
    }
}

/**
 * Twitch takes votes and predictions only on its site, so this opens the same browser chat as the chat settings
 * item, where Twitch's own card has the buttons.
 */
@Composable
private fun VoteBrowserButton(text: String, onClick: () -> Unit, textSize: TextUnit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
    ) {
        Text(text, fontSize = textSize)
    }
}

@Composable
private fun VoteNote(text: String, chatTextSize: Int) {
    val hintSize = chatSp(chatTextSize, 11, 12)
    Text(
        text = text,
        color = TwitchTextSecondary,
        fontSize = hintSize,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp),
    )
}

/** A bar that empties as the time to vote runs out, like the raid countdown. */
@Composable
private fun VoteCountdownBar(countdown: VoteCountdown) {
    val fraction = remember(countdown.endsAtMillis, countdown.durationMillis) {
        Animatable(raidFractionRemaining(countdown.endsAtMillis, System.currentTimeMillis(), countdown.durationMillis))
    }
    LaunchedEffect(fraction) {
        val left = (countdown.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val duration = left.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        fraction.animateTo(0f, tween(durationMillis = duration, easing = LinearEasing))
    }
    LinearProgressIndicator(
        progress = { fraction.value },
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        color = TwitchPurple,
        trackColor = TwitchDivider,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}
