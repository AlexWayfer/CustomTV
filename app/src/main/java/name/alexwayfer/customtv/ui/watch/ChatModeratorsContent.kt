package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.ChatModerator
import name.alexwayfer.customtv.data.ChatModeratorAppearance
import name.alexwayfer.customtv.data.sortedChatModerators
import name.alexwayfer.customtv.ui.theme.TwitchTextSecondary

/** Unknown presence, including every public result, never looks like an offline moderator. */
@Composable
internal fun ChatModeratorsContent(
    moderators: List<ChatModerator>,
    appearances: Map<String, ChatModeratorAppearance> = emptyMap(),
    readableColors: Boolean = true,
    onPick: (ChatModerator) -> Unit,
) {
    val sorted = remember(moderators) { sortedChatModerators(moderators) }
    val sections = remember(sorted) { sorted.groupBy { it.inChat } }
    val showHeadings = sections.keys.any { it != null }
    Column {
        if (sorted.isEmpty()) {
            Text(
                text = stringResource(R.string.chat_moderators_empty),
                color = TwitchTextSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }
        sections.forEach { (inChat, members) ->
            if (showHeadings) {
                Text(
                    text = pluralStringResource(
                        when (inChat) {
                            true -> R.plurals.chat_moderators_in_chat_count
                            false -> R.plurals.chat_moderators_not_in_chat_count
                            null -> R.plurals.chat_moderators_presence_unknown_count
                        },
                        members.size,
                        members.size,
                    ),
                    color = TwitchTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
                )
            }
            members.forEach { moderator ->
                ModeratorRow(moderator, moderator.appearance ?: appearances[moderator.id], readableColors, onPick)
            }
        }
    }
}

@Composable
private fun ModeratorRow(
    moderator: ChatModerator,
    appearance: ChatModeratorAppearance?,
    readableColors: Boolean,
    onPick: (ChatModerator) -> Unit,
) {
    val presence = moderator.inChat?.let {
        stringResource(if (it) R.string.chat_moderator_in_chat else R.string.chat_moderator_not_in_chat)
    }
    ListItem(
        headlineContent = {
            ChatNameWithBadges(
                displayName = moderator.displayName,
                color = chatNickColor(appearance?.color, readableColors),
                badges = appearance?.badges?.badges.orEmpty(),
                badgeUrls = appearance?.badges?.imageUrls.orEmpty(),
                textSize = 16.sp,
                badgeSize = 18.dp,
                userId = moderator.id,
            )
        },
        supportingContent = if (!moderator.displayName.equals(moderator.login, ignoreCase = true)) {
            { Text(moderator.login, color = TwitchTextSecondary) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(role = Role.Button) { onPick(moderator) }.semantics {
            if (presence != null) stateDescription = presence
        },
    )
}
