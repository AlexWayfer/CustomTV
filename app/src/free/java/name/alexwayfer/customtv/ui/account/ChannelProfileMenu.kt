package name.alexwayfer.customtv.ui.account

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.watch.openTwitchReportInBrowser

/**
 * Another channel's actions. The free build has no report sheet of its own, so Report opens Twitch's report page
 * in a Custom Tab with the browser's own Twitch session, full screen like the chatter card's Report.
 */
@Suppress("unused")
@Composable
internal fun ChannelProfileMenu(
    login: String,
    displayName: String,
    userId: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ProfileMenu(description = stringResource(R.string.profile_menu), modifier = modifier) { close ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.report_user)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_report_flag), contentDescription = null) },
            onClick = {
                close()
                openTwitchReportInBrowser(context, login)
            },
        )
    }
}
