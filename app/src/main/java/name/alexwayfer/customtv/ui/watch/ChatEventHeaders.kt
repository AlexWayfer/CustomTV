package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatRaid
import java.text.NumberFormat

@Composable
internal fun ChatRaidLine(
    raid: ChatRaid,
    timestampText: String?,
    textSize: TextUnit,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
) {
    val countLabel = NumberFormat.getIntegerInstance().format(raid.viewerCount)
    val fromLabel = pluralStringResource(R.plurals.chat_raid_from, raid.viewerCount)
    val joinedLabel = pluralStringResource(R.plurals.chat_raid_joined, raid.viewerCount)
    val canceledLabel = stringResource(R.string.chat_raid_canceled)
    val createdLabel = stringResource(R.string.chat_raid_created)
    val annotated = remember(
        timestampText,
        raid.canceled,
        raid.created,
        countLabel,
        raid.fromDisplayName,
        fromLabel,
        joinedLabel,
        canceledLabel,
        createdLabel,
        textSize,
        timestampSize,
    ) {
        buildAnnotatedString {
            appendChatTimestamp(timestampText, timestampSize)
            if (raid.canceled || raid.created) {
                withStyle(SpanStyle(color = Color.White, fontSize = textSize)) {
                    append(if (raid.canceled) canceledLabel else createdLabel)
                }
            } else {
                appendRaidArrival(countLabel, fromLabel, raid.fromDisplayName, joinedLabel, textSize)
            }
        }
    }
    Text(text = annotated, lineHeight = lineHeight)
}

/** "[count] [from] [channel] [joined]": the numbers and the raiding channel in bold. */
private fun AnnotatedString.Builder.appendRaidArrival(
    countLabel: String,
    fromLabel: String,
    fromDisplayName: String,
    joinedLabel: String,
    textSize: TextUnit,
) {
    val plain = SpanStyle(color = Color.White, fontSize = textSize)
    val bold = plain.copy(fontWeight = FontWeight.Bold)
    withStyle(bold) {
        append(countLabel)
    }
    withStyle(plain) {
        append(" ")
        append(fromLabel)
        append(" ")
    }
    withStyle(bold) {
        append(fromDisplayName)
    }
    withStyle(plain) {
        append(" ")
        append(joinedLabel)
    }
}

@Composable
internal fun ChatAnnouncementHeader(
    accent: Color,
    chatTextSize: Int,
    timestampSize: TextUnit,
    lineHeight: TextUnit,
) {
    val iconSize = chatDp(chatTextSize, 12, 14)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_megaphone),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = stringResource(R.string.chat_announcement),
            color = accent,
            fontSize = timestampSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = lineHeight,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
