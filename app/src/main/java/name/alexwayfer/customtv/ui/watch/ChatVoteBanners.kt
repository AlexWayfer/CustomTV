package name.alexwayfer.customtv.ui.watch

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelPoll
import name.alexwayfer.customtv.chat.ChannelPollStatus
import name.alexwayfer.customtv.chat.ChannelPrediction
import name.alexwayfer.customtv.chat.ChannelPredictionOutcome
import name.alexwayfer.customtv.chat.ChannelPredictionStatus
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.placementAnimation
import name.alexwayfer.customtv.chat.pollChoicesInOrder
import name.alexwayfer.customtv.chat.pollLeaderIds
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

@Composable
internal fun ChatPollBanner(
    poll: ChannelPoll,
    channelId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chatTextSize: Int,
    onOpenTwitchChat: () -> Unit,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    val active = poll.status == ChannelPollStatus.Active
    val leaders = pollLeaderIds(poll)
    val voting = rememberPollVoting(poll, channelId, expanded)
    val lockedPick = pollLockedPick()
    val tap = pollChoiceTap(active, voting, premiumLocked = lockedPick != null)
    // With a vote of its own the card needs no browser.
    val offerBrowser = active && voting == null
    ChatVoteCard(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        label = stringResource(if (active) R.string.chat_poll else R.string.chat_poll_ended),
        title = poll.title,
        chatTextSize = chatTextSize,
        countdown = if (active) VoteCountdown(poll.endsAtMillis, poll.durationMillis) else null,
        browserLabel = if (offerBrowser) stringResource(R.string.chat_poll_vote_in_browser) else null,
        note = if (offerBrowser) pollBrowserNote() else null,
        action = if (active && voting != null) {
            { PollVoteAction(voting = voting, chatTextSize = chatTextSize) }
        } else {
            null
        },
        headerAction = { PollOwnerMenu(channelId = channelId, chatTextSize = chatTextSize) },
        onOpenTwitchChat = onOpenTwitchChat,
        onHide = onHide,
        bottomInset = bottomInset,
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // An ended poll sorts its results, and each row slides to its new place.
            pollChoicesInOrder(poll).forEach { choice ->
                key(choice.id) {
                    PollChoiceRow(
                        choice = choice,
                        totalVotes = poll.totalVotes,
                        leader = choice.id in leaders,
                        winner = !active && choice.id in leaders,
                        tap = tap,
                        picked = voting?.selectedChoiceId == choice.id,
                        voted = voting?.votedChoiceId == choice.id,
                        onPick = { if (voting != null) voting.onSelect(choice.id) else lockedPick?.invoke() },
                        chatTextSize = chatTextSize,
                        modifier = Modifier.placementAnimation(VOTE_ANIMATION_MS),
                    )
                }
            }
        }
    }
}

/** Vote, and on its right what became of the vote: "Voted!", "Already voted!", or that Twitch did not take it. */
@Composable
private fun PollVoteAction(voting: PollVoting, chatTextSize: Int) {
    val textSize = chatSp(chatTextSize, 12, 14)
    val status = when (pollVoteStatus(voting)) {
        PollVoteStatus.Voted -> stringResource(R.string.chat_poll_voted)
        PollVoteStatus.AlreadyVoted -> stringResource(R.string.chat_poll_already_voted)
        PollVoteStatus.Failed -> stringResource(R.string.chat_poll_vote_failed)
        PollVoteStatus.None -> null
    }
    Row(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = voting.onVote,
            enabled = pollVoteEnabled(voting),
        ) {
            Text(stringResource(R.string.chat_poll_vote), fontSize = textSize)
        }
        AnimatedContent(
            targetState = status,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            label = "poll vote status",
        ) { text ->
            Text(
                text = text.orEmpty(),
                color = TwitchTextSecondary,
                fontSize = textSize,
            )
        }
    }
}

internal enum class PredictionBrowserOffer {
    Browser,
    OwnChannel,
    None,
}

/**
 * An open prediction offers the browser, except on the viewer's own channel: Twitch does not let
 * a streamer predict there, so that notice says so instead. A result offers neither.
 */
internal fun predictionBrowserOffer(active: Boolean, channelLogin: String, selfLogin: String?): PredictionBrowserOffer =
    when {
        !active -> PredictionBrowserOffer.None
        channelLogin.equals(selfLogin, ignoreCase = true) -> PredictionBrowserOffer.OwnChannel
        else -> PredictionBrowserOffer.Browser
    }

@Composable
internal fun ChatPredictionBanner(
    prediction: ChannelPrediction,
    channelId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    channelLogin: String,
    selfLogin: String?,
    pointsIconUrl: String?,
    chatTextSize: Int,
    onOpenTwitchChat: () -> Unit,
    onHide: () -> Unit,
    bottomInset: Dp,
) {
    val active = prediction.status == ChannelPredictionStatus.Active
    val entry = rememberPredictionEntry(prediction, channelId, expanded)
    // With predicting of its own the card needs no browser.
    val offer = predictionBrowserOffer(active, channelLogin, selfLogin)
        .takeUnless { it == PredictionBrowserOffer.Browser && entry != null } ?: PredictionBrowserOffer.None
    ChatVoteCard(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        label = stringResource(if (active) R.string.chat_prediction else R.string.chat_prediction_result),
        title = prediction.title,
        chatTextSize = chatTextSize,
        countdown = if (active) VoteCountdown(prediction.endsAtMillis, prediction.windowMillis) else null,
        browserLabel = stringResource(R.string.chat_prediction_predict_in_browser)
            .takeIf { offer == PredictionBrowserOffer.Browser },
        note = when (offer) {
            PredictionBrowserOffer.Browser -> stringResource(R.string.chat_vote_browser_hint)
            PredictionBrowserOffer.OwnChannel -> stringResource(R.string.chat_prediction_own_channel)
            PredictionBrowserOffer.None -> null
        },
        action = if (active && entry != null) {
            {
                Button(
                    onClick = entry.onPredict,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.chat_prediction_predict),
                        fontSize = chatSp(chatTextSize, normal = 12, large = 14),
                    )
                }
            }
        } else {
            null
        },
        headerAction = {
            PredictionModMenu(prediction, channelId, expanded, chatDp(chatTextSize, normal = 20, large = 22))
        },
        onOpenTwitchChat = onOpenTwitchChat,
        onHide = onHide,
        bottomInset = bottomInset,
    ) {
        prediction.outcomes.forEachIndexed { index, outcome ->
            key(outcome.id) {
                PredictionOutcomeRow(
                    number = index + 1,
                    color = predictionOutcomeColor(index, prediction.outcomes.size),
                    outcome = outcome,
                    winner = outcome.id == prediction.winningOutcomeId,
                    picked = entry?.ownOutcomeId == outcome.id,
                    pointsIconUrl = pointsIconUrl,
                    chatTextSize = chatTextSize,
                )
            }
        }
    }
}

/**
 * An outcome with its channel points, which run to each new total like a stream's viewers. The winning outcome gets
 * the trophy, as on Twitch; the viewer's outcome gets the check.
 */
@Composable
private fun PredictionOutcomeRow(
    number: Int,
    color: PredictionOutcomeColor,
    outcome: ChannelPredictionOutcome,
    winner: Boolean,
    picked: Boolean,
    pointsIconUrl: String?,
    chatTextSize: Int,
) {
    val bodySize = chatSp(chatTextSize, 14, 16)
    val iconSize = chatDp(chatTextSize, 14, 16)
    val locale = LocalConfiguration.current.locales[0]
    val points = NumberFormat.getIntegerInstance(locale).format(outcome.points)
    val shownPoints = NumberFormat.getIntegerInstance(locale).format(animateCount(outcome.points, VOTE_ANIMATION_MS))
    val outcomeDescription = pluralStringResource(
        R.plurals.chat_prediction_outcome_description,
        outcome.points.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        outcome.title,
        points,
    )
    val winnerLabel = stringResource(R.string.chat_prediction_winner)
    val yourPrediction = stringResource(R.string.chat_prediction_your_outcome)
    // TalkBack reads the final total, not each step of the count.
    val description = listOfNotNull(winnerLabel.takeIf { winner }, yourPrediction.takeIf { picked }, outcomeDescription)
        .joinToString(". ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (winner) {
                WinnerTrophy(Modifier.padding(end = 6.dp).size(iconSize))
            }
            PredictionOutcomeBadge(
                number = number,
                color = color,
                size = chatDp(chatTextSize, normal = 16, large = 18),
                textSize = chatSp(chatTextSize, normal = 10, large = 11),
            )
            Text(
                text = outcome.title,
                color = TwitchText,
                fontSize = bodySize,
                fontWeight = if (winner) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
            )
            AnimatedVisibility(
                visible = picked,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                PickedCheck(Modifier.padding(start = 6.dp).size(iconSize))
            }
        }
        ChannelPointsGlyph(pointsIconUrl, iconSize)
        Text(
            text = shownPoints,
            color = TwitchText,
            fontSize = bodySize,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Poll bars, prediction points, and the hype train's fill follow changes faster than a stream's viewer count. */
internal const val VOTE_ANIMATION_MS = 1000

/** The winning choice or outcome, as on Twitch: it pulses a few times as it appears, then stays still. */
@Composable
internal fun WinnerTrophy(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionOff = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (motionOff) return@LaunchedEffect
        repeat(TROPHY_PULSES) {
            scale.animateTo(TROPHY_PULSE_SCALE, tween(TROPHY_PULSE_MILLIS))
            scale.animateTo(1f, tween(TROPHY_PULSE_MILLIS))
        }
    }
    Icon(
        painter = painterResource(R.drawable.ic_chat_trophy),
        contentDescription = null,
        tint = TwitchText,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

private const val TROPHY_PULSES = 4
private const val TROPHY_PULSE_SCALE = 1.3f
private const val TROPHY_PULSE_MILLIS = 500

/** The viewer's vote or prediction: a heavier check than the Material icon, so it stands out beside the title. */
@Composable
internal fun PickedCheck(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val check = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.54f)
            lineTo(size.width * 0.40f, size.height * 0.78f)
            lineTo(size.width * 0.86f, size.height * 0.24f)
        }
        drawPath(check, TwitchPurple, style = stroke)
    }
}

@Composable
private fun ChannelPointsGlyph(url: String?, size: Dp) {
    if (!url.isNullOrBlank()) {
        SharedEmoteImage(url = url, contentDescription = null, size = size)
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_chat_channel_points),
            contentDescription = null,
            tint = TwitchText,
            modifier = Modifier.size(size),
        )
    }
}
