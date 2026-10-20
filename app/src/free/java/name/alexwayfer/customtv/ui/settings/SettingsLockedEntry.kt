package name.alexwayfer.customtv.ui.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.PremiumBannerRequest
import name.alexwayfer.customtv.ui.PremiumLockIcon

/**
 * A settings page Premium has, as the free list shows it: a lock where the arrow would be, and a tap shows the
 * Premium banner. [source] names the row for the log.
 */
@Composable
internal fun SettingsLockedEntry(icon: Painter, title: String, source: String) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(stringResource(R.string.premium_locked)) },
        // The line under the title already says it; TalkBack reads it once.
        trailingContent = { PremiumLockIcon(Modifier.clearAndSetSemantics {}) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { PremiumBannerRequest.send(source) },
    )
}
