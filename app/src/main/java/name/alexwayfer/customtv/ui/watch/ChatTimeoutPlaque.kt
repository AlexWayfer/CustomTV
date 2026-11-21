package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchText
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

@Composable
internal fun ChatTimeoutPlaque(seconds: Long, sendRefused: Boolean) {
    Column(Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        ChatTimeoutStatus(seconds)
        if (sendRefused) {
            Text(
                stringResource(R.string.chat_timeout_send_refused),
                color = TwitchTextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ChatTimeoutStatus(seconds: Long) {
    val countdown = seconds.takeIf { it > 0L }?.let(::formatChatTimeoutCountdown)
    val description = countdown?.let { stringResource(R.string.chat_send_timeout_countdown, it) }
    val semantics = if (description == null) {
        Modifier.semantics(mergeDescendants = true) {}
    } else {
        Modifier.clearAndSetSemantics { contentDescription = description }
    }
    Row(
        modifier = semantics,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_moderation_timeout),
            contentDescription = null,
            tint = TwitchTextSecondary,
        )
        val title = stringResource(R.string.chat_timeout_title)
        if (countdown == null) {
            Column {
                Text(title, color = TwitchText, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.chat_timeout_retry_hint),
                    color = TwitchTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Text(
                title,
                modifier = Modifier.weight(1f, fill = false),
                color = TwitchTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                countdown,
                color = TwitchText,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
        }
    }
}
