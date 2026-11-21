package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

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
    ChatNoticeCard(
        expanded = expanded,
        bottomInset = bottomInset,
        summary = { allLines, fold, _ ->
            ChatNoticeHeader(expanded, onExpandedChange, chatTextSize, expandLabel) {
                ChatNoticeTitle(label, title, chatTextSize, allLines, fold)
                headerAction?.invoke()
            }
        },
        footer = { countdown?.let { ChatNoticeCountdownBar(it.endsAtMillis, it.durationMillis) } },
    ) {
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

internal class VoteCountdown(val endsAtMillis: Long, val durationMillis: Long)

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
