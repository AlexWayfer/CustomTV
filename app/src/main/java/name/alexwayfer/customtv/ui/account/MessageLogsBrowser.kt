package name.alexwayfer.customtv.ui.account

import android.content.Context
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.watch.openTwitchPageInBrowser

/** The profile menu's Open message logs, on the account's own profile and another channel's, in both editions. */
@Composable
internal fun MessageLogsMenuItem(userLogin: String, close: () -> Unit) {
    val context = LocalContext.current
    val sheetHeightPx = aboutBrowserSheetHeightPx(LocalWindowInfo.current.containerSize.height)
    DropdownMenuItem(
        text = { Text(stringResource(R.string.profile_message_logs)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_chat_history), contentDescription = null) },
        onClick = {
            close()
            openMessageLogsInBrowser(context, userLogin, sheetHeightPx)
        },
    )
}

/** The Lurkology page with the user's messages; the user picks the channel there. */
internal fun messageLogsUrl(userLogin: String): String =
    "https://lurkology.com/logs?u=${userLogin.trim().removePrefix("#").lowercase()}"

/** Opens the user's message logs in a Custom Tab as a sheet, the same height as the profile's About on Twitch. */
internal fun openMessageLogsInBrowser(
    context: Context,
    userLogin: String,
    sheetHeightPx: Int,
) = openTwitchPageInBrowser(context, messageLogsUrl(userLogin), sheetHeightPx)
