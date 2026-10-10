package name.alexwayfer.customtv.ui.watch

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelPoll
import name.alexwayfer.customtv.chat.ChannelPollChoice
import name.alexwayfer.customtv.chat.ChannelPollStatus
import name.alexwayfer.customtv.chat.ChannelPrediction
import name.alexwayfer.customtv.chat.ChannelPredictionOutcome
import name.alexwayfer.customtv.chat.ChannelPredictionStatus
import name.alexwayfer.customtv.ui.components.CardFoldSpring
import name.alexwayfer.customtv.ui.components.SharedEmoteImage
import name.alexwayfer.customtv.ui.components.collapsibleTextMaxLines
import name.alexwayfer.customtv.ui.components.placementAnimation
import name.alexwayfer.customtv.ui.components.revealLines
import name.alexwayfer.customtv.chat.pollChoicesInOrder
import name.alexwayfer.customtv.chat.pollChoicePercent
import name.alexwayfer.customtv.chat.pollChoiceShare
import name.alexwayfer.customtv.chat.pollLeaderIds
import name.alexwayfer.customtv.chat.raidFractionRemaining
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurface
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat
import kotlin.math.roundToInt

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
    val pickable = pollChoicesPickable(active, voting)
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
        note = if (offerBrowser) stringResource(R.string.chat_vote_browser_hint) else null,
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
                        pickable = pickable,
                        picked = voting?.selectedChoiceId == choice.id,
                        voted = voting?.votedChoiceId == choice.id,
                        onPick = { voting?.onSelect(choice.id) },
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

/**
 * A choice with its share of the votes. The numbers switch at once; the bar slides to its new share. While the
 * viewer can vote, a radio button picks the choice. Once the poll ends, the leaders get the trophy, as on Twitch; the
 * viewer's choice gets the check.
 */
@Composable
private fun PollChoiceRow(
    choice: ChannelPollChoice,
    totalVotes: Int,
    leader: Boolean,
    winner: Boolean,
    pickable: Boolean,
    picked: Boolean,
    voted: Boolean,
    onPick: () -> Unit,
    chatTextSize: Int,
    modifier: Modifier = Modifier,
) {
    val bodySize = chatSp(chatTextSize, 14, 16)
    val iconSize = chatDp(chatTextSize, 14, 16)
    val locale = LocalConfiguration.current.locales[0]
    val percent = pollChoicePercent(choice.votes, totalVotes)
    val votes = NumberFormat.getIntegerInstance(locale).format(choice.votes)
    val choiceDescription = pluralStringResource(
        R.plurals.chat_poll_choice_description,
        choice.votes,
        choice.title,
        percent,
        votes,
    )
    val winnerLabel = stringResource(R.string.chat_prediction_winner)
    val yourVote = stringResource(R.string.chat_poll_your_vote)
    val description = listOfNotNull(winnerLabel.takeIf { winner }, yourVote.takeIf { voted }, choiceDescription).joinToString(". ")
    val share by animateFloatAsState(
        targetValue = pollChoiceShare(choice.votes, totalVotes),
        animationSpec = tween(VOTE_ANIMATION_MS),
        label = "poll share",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(TwitchDivider)
            .drawBehind {
                drawRect(PollShareColor, size = Size(size.width * share, size.height))
            }
            .then(
                if (pickable) {
                    Modifier.selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (pickable) {
                    role = Role.RadioButton
                    selected = picked
                    onClick { onPick(); true }
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = pickable,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                RadioButton(
                    selected = picked,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TwitchPurple,
                        unselectedColor = TwitchTextSecondary,
                    ),
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = winner,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    WinnerTrophy(Modifier.padding(end = 6.dp).size(iconSize))
                }
                Text(
                    text = choice.title,
                    color = TwitchText,
                    fontSize = bodySize,
                    fontWeight = if (leader) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(
                    visible = voted,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    PickedCheck(Modifier.padding(start = 6.dp).size(iconSize))
                }
            }
            Text(
                text = pluralStringResource(R.plurals.chat_poll_choice_result, choice.votes, percent, votes),
                color = TwitchText,
                fontSize = bodySize,
                modifier = Modifier.padding(start = 8.dp),
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
                    Text(stringResource(R.string.chat_prediction_predict), fontSize = chatSp(chatTextSize, 12, 14))
                }
            }
        } else {
            null
        },
        headerAction = null,
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
                size = chatDp(chatTextSize, 16, 18),
                textSize = chatSp(chatTextSize, 10, 11),
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

private val PollShareColor = TwitchPurple.copy(alpha = 0.45f)

/** Poll bars, prediction points, and the hype train's fill follow changes faster than a stream's viewer count. */
internal const val VOTE_ANIMATION_MS = 1000

private class VoteCountdown(val endsAtMillis: Long, val durationMillis: Long)

/** The card both notices share: a tap on the header shows the choices, like the pinned message. */
@Composable
private fun ChatVoteCard(
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
    val hintSize = chatSp(chatTextSize, 11, 12)
    val titleSize = chatSp(chatTextSize, 14, 16)
    val expandLabel = stringResource(if (expanded) R.string.chat_vote_collapse else R.string.chat_vote_expand)
    // The title keeps every line while the card folds, and the fold cuts them off from the bottom.
    val transition = updateTransition(expanded, label = "voteBanner")
    val fold by transition.animateFloat({ CardFoldSpring }, label = "voteFold") { if (it) 1f else 0f }
    val allLines = transition.currentState || transition.targetState
    var titleFirstLineBottomPx by remember { mutableIntStateOf(0) }
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
                    modifier = Modifier.revealLines({ titleFirstLineBottomPx }, { fold }),
                )
            }
            headerAction?.invoke()
            NoticeExpandArrow(
                expanded = expanded,
                contentDescription = expandLabel,
                tint = TwitchTextSecondary,
                modifier = Modifier.size(chatDp(chatTextSize, 20, 22)),
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
                // Twitch takes votes and predictions only on its site, so this opens the same
                // browser chat as the chat settings item, where Twitch's own card has the buttons.
                browserLabel?.let { label ->
                    Button(
                        onClick = onOpenTwitchChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                    ) {
                        Text(label, fontSize = headerSize)
                    }
                }
                action?.invoke()
                note?.let { text ->
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

/** The winning choice or outcome, as on Twitch: it pulses a few times as it appears, then stays still. */
@Composable
private fun WinnerTrophy(modifier: Modifier = Modifier) {
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
private fun PickedCheck(modifier: Modifier = Modifier) {
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
