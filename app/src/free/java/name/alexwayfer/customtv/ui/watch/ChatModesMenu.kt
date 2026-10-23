package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.chat.ChatBadge
import name.alexwayfer.customtv.chat.ChatRoomModes
import name.alexwayfer.customtv.ui.PremiumBannerRequest
import name.alexwayfer.customtv.ui.PremiumLockIcon

/** Changing the chat modes is Premium: the streamer and moderators see it locked, by their own badges. */
@Composable
internal fun rememberChatModeControl(channelId: String?, ownBadges: List<ChatBadge>?): ChatModeControl? {
    val moderates = ownBadgesModerate(ownBadges)
    return remember(moderates) {
        if (moderates) ChatModeControl.Locked { PremiumBannerRequest.send("chat modes") } else null
    }
}

/** The free build has no chat modes page. */
@Suppress("FunctionOnlyReturningConstant")
@Composable
internal fun rememberChatModesSubpage(roomModes: ChatRoomModes, control: ChatModeControl?): ChatMenuSubpage? = null

/** Chat modes as Premium shows them in the chat menu, with a lock: a tap shows the Premium banner. */
@Composable
internal fun ChatModesRow(
    roomModes: ChatRoomModes,
    control: ChatModeControl?,
    page: ChatMenuSubpage?,
    openSubpage: (ChatMenuSubpage) -> Unit,
) {
    val locked = control as? ChatModeControl.Locked ?: return
    ChatSettingsRow(
        label = stringResource(R.string.chat_modes_title),
        icon = painterResource(R.drawable.ic_moderation_shield_outline),
        modifier = Modifier.clickable(role = Role.Button, onClick = locked.showPremium),
        trailing = { PremiumLockIcon() },
    )
}
