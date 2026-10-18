package name.alexwayfer.customtv.ui.settings

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.links.appLinksNeedSetup
import name.alexwayfer.customtv.links.openAppLinkSettings
import name.alexwayfer.customtv.links.rememberAppLinksEnabled

/**
 * The row that opens Android's Open by default page of the app, where twitch.tv links are handed to it, and says
 * whether they are. Older Android versions ask on each link, so they show nothing here.
 */
@Composable
internal fun AppLinksSettings() {
    if (!appLinksNeedSetup(Build.VERSION.SDK_INT)) return
    val context = LocalContext.current
    val enabled = rememberAppLinksEnabled()
    SettingsSection(stringResource(R.string.settings_section_app_links))
    ListItem(
        modifier = Modifier
            .clickable(role = Role.Button) { openAppLinkSettings(context) }
            .animateContentSize(),
        leadingContent = { Icon(painterResource(R.drawable.ic_profile_link), contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.app_links_setting)) },
        supportingContent = {
            Text(stringResource(if (enabled) R.string.app_links_setting_on else R.string.app_links_setting_off))
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
