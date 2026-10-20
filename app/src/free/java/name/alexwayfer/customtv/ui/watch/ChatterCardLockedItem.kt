package name.alexwayfer.customtv.ui.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import name.alexwayfer.customtv.ui.PremiumBannerRequest
import name.alexwayfer.customtv.ui.PremiumLockIcon

/**
 * A chatter card row for a Premium action, as Premium shows it, with a lock: a tap shows the Premium banner over the
 * card. [source] names the row for the log.
 */
@Composable
internal fun ChatterCardLockedItem(icon: Painter, title: String, source: String, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { PremiumLockIcon() },
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { PremiumBannerRequest.send(source) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
