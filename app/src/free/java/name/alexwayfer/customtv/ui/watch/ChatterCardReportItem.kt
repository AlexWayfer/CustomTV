package name.alexwayfer.customtv.ui.watch

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R

/**
 * Reports this chatter on twitch.tv. The free build has no report sheet of its own, so Report opens Twitch's report
 * page in a Custom Tab with the browser's own Twitch session. Unlike the other chat pages it opens full screen: in a
 * shorter sheet, the mobile site's own bottom bar covers the form's Next button and the page does not scroll it free.
 */
@Suppress("unused")
@Composable
internal fun ChatterCardReportItem(
    login: String,
    displayName: String,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(stringResource(R.string.report_user)) },
        leadingContent = { Icon(painterResource(R.drawable.ic_report_flag), contentDescription = null) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { openTwitchReportInBrowser(context, login) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

internal fun twitchReportUrl(login: String): String = "https://www.twitch.tv/$login/report"

/** Opens Twitch's report page for [login] full screen; a sheet leaves the form's Next button under the site's bar. */
internal fun openTwitchReportInBrowser(context: Context, login: String) =
    openTwitchPageInBrowser(context, twitchReportUrl(login), sheetHeightPx = 0)
