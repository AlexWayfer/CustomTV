package name.alexwayfer.customtv.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.channel.channelNameWithCollaborators
import name.alexwayfer.customtv.channel.displayNameLabel
import name.alexwayfer.customtv.ui.theme.TwitchText

@Composable
internal fun ChannelListTitle(
    displayName: String,
    login: String,
    collaboratorCount: Int?,
    modifier: Modifier = Modifier,
) {
    val label = displayNameLabel(displayName, login)
    val text = channelNameWithCollaborators(label, collaboratorCount)
    val description = collaboratorCount?.let { count ->
        pluralStringResource(R.plurals.channel_collaboration_with, count, label, count)
    }
    Text(
        text = text,
        modifier = if (description == null) {
            modifier
        } else {
            modifier.clearAndSetSemantics { contentDescription = description }
        },
        color = TwitchText,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
