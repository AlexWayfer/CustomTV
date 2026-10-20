package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChannelPollChoice
import name.alexwayfer.customtv.chat.pollChoicePercent
import name.alexwayfer.customtv.chat.pollChoiceShare
import name.alexwayfer.customtv.ui.theme.TwitchDivider
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary
import java.text.NumberFormat

/**
 * A choice with its share of the votes. The numbers switch at once; the bar slides to its new share. While the
 * viewer can vote, a radio button picks the choice; where only Premium can, a tap on it calls [onPick] to offer
 * Premium. Once the poll ends, the leaders get the trophy, as on Twitch; the viewer's choice gets the check.
 */
@Composable
internal fun PollChoiceRow(
    choice: ChannelPollChoice,
    totalVotes: Int,
    leader: Boolean,
    winner: Boolean,
    tap: PollChoiceTap,
    picked: Boolean,
    voted: Boolean,
    onPick: () -> Unit,
    chatTextSize: Int,
    modifier: Modifier = Modifier,
) {
    val pickable = tap == PollChoiceTap.Pick
    val premiumLabel = stringResource(R.string.premium_locked)
    val bodySize = chatSp(chatTextSize, 14, 16)
    val locale = LocalConfiguration.current.locales[0]
    val percent = pollChoicePercent(choice.votes, totalVotes)
    val votes = NumberFormat.getIntegerInstance(locale).format(choice.votes)
    val description = pollChoiceDescription(choice, percent, votes, winner, voted)
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
                when (tap) {
                    PollChoiceTap.Pick ->
                        Modifier.selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
                    PollChoiceTap.Premium -> Modifier.clickable(onClick = onPick)
                    PollChoiceTap.None -> Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                when (tap) {
                    PollChoiceTap.Pick -> {
                        role = Role.RadioButton
                        selected = picked
                        onClick { onPick(); true }
                    }
                    // TalkBack offers the tap as what it leads to: the choice is in Premium.
                    PollChoiceTap.Premium -> {
                        role = Role.Button
                        onClick(label = premiumLabel) { onPick(); true }
                    }
                    PollChoiceTap.None -> Unit
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PollChoiceRadio(visible = pickable, picked = picked)
            PollChoiceTitle(choice.title, leader, winner, voted, chatTextSize)
            Text(
                text = pluralStringResource(R.plurals.chat_poll_choice_result, choice.votes, percent, votes),
                color = TwitchText,
                fontSize = bodySize,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** What TalkBack reads for the choice: the winner, the viewer's vote, then the title, share, and votes. */
@Composable
private fun pollChoiceDescription(
    choice: ChannelPollChoice,
    percent: Int,
    votes: String,
    winner: Boolean,
    voted: Boolean,
): String {
    val choiceDescription = pluralStringResource(
        R.plurals.chat_poll_choice_description,
        choice.votes,
        choice.title,
        percent,
        votes,
    )
    val winnerLabel = stringResource(R.string.chat_prediction_winner)
    val yourVote = stringResource(R.string.chat_poll_your_vote)
    return listOfNotNull(winnerLabel.takeIf { winner }, yourVote.takeIf { voted }, choiceDescription)
        .joinToString(". ")
}

@Composable
private fun RowScope.PollChoiceRadio(visible: Boolean, picked: Boolean) {
    AnimatedVisibility(
        visible = visible,
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
}

/** The choice's title, bold while it leads, with the trophy before it and the viewer's check after it. */
@Composable
private fun RowScope.PollChoiceTitle(
    title: String,
    leader: Boolean,
    winner: Boolean,
    voted: Boolean,
    chatTextSize: Int,
) {
    val bodySize = chatSp(chatTextSize, 14, 16)
    val iconSize = chatDp(chatTextSize, 14, 16)
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
            text = title,
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
}

private val PollShareColor = TwitchPurple.copy(alpha = 0.45f)
