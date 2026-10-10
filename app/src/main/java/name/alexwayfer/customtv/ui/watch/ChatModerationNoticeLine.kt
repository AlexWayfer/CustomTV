package name.alexwayfer.customtv.ui.watch

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatModerationNotice
import name.alexwayfer.customtv.chat.formatTimeoutDuration
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** A gray line that a chatter was timed out or banned, led by the moderator when the user moderates the channel. */
@Composable
internal fun ChatModerationNoticeLine(
    notice: ChatModerationNotice,
    timestampText: String?,
    textSize: TextUnit,
    lineHeight: TextUnit,
) {
    val moderator = notice.moderatorName
    val duration = notice.timeoutSeconds?.let(::formatTimeoutDuration)
    val text = when {
        moderator != null && duration != null ->
            stringResource(R.string.chat_moderator_timed_out, moderator, notice.targetName, duration)
        moderator != null -> stringResource(R.string.chat_moderator_banned, moderator, notice.targetName)
        duration != null -> stringResource(R.string.chat_user_timed_out, notice.targetName, duration)
        else -> stringResource(R.string.chat_user_banned, notice.targetName)
    }
    Text(
        text = if (timestampText != null) "$timestampText $text" else text,
        color = TwitchTextSecondary,
        fontSize = textSize,
        fontWeight = FontWeight.Medium,
        lineHeight = lineHeight,
    )
}
