package name.alexwayfer.customtv.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import name.alexwayfer.customtv.ui.theme.TwitchPurple

internal class ChatDropsNotice(
    val key: String,
    val content: @Composable (
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        chatTextSize: Int,
        bottomInset: Dp,
    ) -> Unit,
)

internal data class ChatDropsNotices(
    val campaign: ChatDropsNotice? = null,
    val claim: ChatDropsNotice? = null,
    val received: ChatDropsNotice? = null,
    val hasUnreadReceipt: Boolean = false,
) {
    val showsMenuDot: Boolean get() = claim != null || hasUnreadReceipt

    operator fun get(page: ChatNoticePage): ChatDropsNotice? = when (page) {
        ChatNoticePage.Drops -> campaign
        ChatNoticePage.DropsClaim -> claim
        ChatNoticePage.DropsReceived -> received
        else -> null
    }
}

internal val LocalChatDropsNotices = compositionLocalOf { ChatDropsNotices() }

@Composable
internal fun BoxScope.ChatMenuIconDot(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopEnd),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Badge(containerColor = TwitchPurple)
    }
}
