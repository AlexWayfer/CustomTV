package name.alexwayfer.customtv.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import name.alexwayfer.customtv.auth.TwitchAccount

@Composable
internal fun AccountScreen(
    account: TwitchAccount,
    onLeave: () -> Unit,
    onLogOut: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (ProfileVideoPlayback) -> Unit,
) {
    var confirmLogOut by remember { mutableStateOf(false) }
    AccountAboutScreen(
        account = account,
        onLeave = onLeave,
        onLogOut = { confirmLogOut = true },
        onOpenChannel = onOpenChannel,
        onOpenVideo = onOpenVideo,
    )
    if (confirmLogOut) {
        LogOutConfirmDialog(
            onConfirm = {
                confirmLogOut = false
                onLogOut()
            },
            onDismiss = { confirmLogOut = false },
        )
    }
}
