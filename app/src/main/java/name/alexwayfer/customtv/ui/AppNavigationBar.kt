package name.alexwayfer.customtv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle as FilledAccountCircle
import androidx.compose.material.icons.filled.Email as FilledEmail
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.outlined.AccountCircle as OutlinedAccountCircle
import androidx.compose.material.icons.outlined.Email as OutlinedEmail
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.auth.TwitchAccount
import name.alexwayfer.customtv.chat.whispersNavEnabled
import name.alexwayfer.customtv.chat.whispersUnreadCount
import name.alexwayfer.customtv.ui.components.UnreadCountBadge

internal enum class AppSection {
    Home,
    Whispers,
    Account,
    ChannelProfile,
    Settings,
}

/** One destination of the app navigation, drawn the same at the bottom or on the rail. */
private class NavEntry(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
)

/**
 * The app sections: a bar along the bottom, or with [rail] a rail along the start edge, clear of the system bars
 * and a camera cutout on that side.
 */
@Composable
internal fun AppNavigationBar(
    rail: Boolean,
    section: AppSection,
    account: TwitchAccount?,
    onHome: () -> Unit,
    onWhispers: () -> Unit,
    onAccount: () -> Unit,
    onLogIn: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loggedIn = account != null
    val entries = buildList {
        add(
            NavEntry(
                selected = section == AppSection.Home,
                onClick = onHome,
                icon = { NavIcon(section == AppSection.Home, Icons.Filled.FilledHome, Icons.Outlined.OutlinedHome) },
                label = { NavLabel(stringResource(R.string.nav_home), section == AppSection.Home) },
            ),
        )
        if (whispersNavEnabled()) {
            val unread = if (loggedIn) whispersUnreadCount() else 0
            val unreadText = pluralStringResource(R.plurals.whispers_unread, unread, unread)
            add(
                NavEntry(
                    selected = section == AppSection.Whispers,
                    onClick = if (loggedIn) onWhispers else onLogIn,
                    icon = {
                        BadgedBox(
                            badge = {
                                UnreadCountBadge(
                                    count = unread,
                                    modifier = Modifier.semantics {
                                        if (unread > 0) contentDescription = unreadText
                                    },
                                )
                            },
                        ) {
                            NavIcon(
                                section == AppSection.Whispers,
                                Icons.Filled.FilledEmail,
                                Icons.Outlined.OutlinedEmail,
                            )
                        }
                    },
                    label = { NavLabel(stringResource(R.string.nav_whispers), section == AppSection.Whispers) },
                ),
            )
        }
        add(
            NavEntry(
                selected = section == AppSection.Account,
                onClick = if (loggedIn) onAccount else onLogIn,
                icon = {
                    NavIcon(
                        section == AppSection.Account,
                        Icons.Filled.FilledAccountCircle,
                        Icons.Outlined.OutlinedAccountCircle,
                    )
                },
                label = {
                    NavLabel(
                        stringResource(if (loggedIn) R.string.nav_profile else R.string.log_in),
                        section == AppSection.Account,
                    )
                },
            ),
        )
        add(
            NavEntry(
                selected = section == AppSection.Settings,
                onClick = onSettings,
                icon = {
                    NavIcon(
                        section == AppSection.Settings,
                        Icons.Filled.FilledSettings,
                        Icons.Outlined.OutlinedSettings,
                    )
                },
                label = { NavLabel(stringResource(R.string.settings), section == AppSection.Settings) },
            ),
        )
    }
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    if (rail) {
        val itemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedIconColor = iconColor,
            selectedTextColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedTextColor = iconColor,
        )
        NavigationRail(
            modifier = modifier,
            // The bottom bar's color, so turning the phone only moves the navigation.
            containerColor = NavigationBarDefaults.containerColor,
            windowInsets = WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        ) {
            // Grouped in the middle of the edge, where a thumb reaches a phone held on its side.
            Spacer(Modifier.weight(1f))
            entries.forEach { entry ->
                NavigationRailItem(
                    selected = entry.selected,
                    onClick = entry.onClick,
                    icon = entry.icon,
                    label = entry.label,
                    colors = itemColors,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    } else {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedIconColor = iconColor,
            selectedTextColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedTextColor = iconColor,
        )
        NavigationBar(modifier) {
            entries.forEach { entry ->
                NavigationBarItem(
                    selected = entry.selected,
                    onClick = entry.onClick,
                    icon = entry.icon,
                    label = entry.label,
                    colors = itemColors,
                )
            }
        }
    }
}

@Composable
private fun NavIcon(selected: Boolean, filled: ImageVector, outlined: ImageVector) {
    Icon(
        imageVector = if (selected) filled else outlined,
        contentDescription = null,
    )
}

@Composable
private fun NavLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
