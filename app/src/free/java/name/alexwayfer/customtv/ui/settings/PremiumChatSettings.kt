package name.alexwayfer.customtv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

@Suppress("unused")
@Composable
internal fun LinkPreviewSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit

@Suppress("unused")
@Composable
internal fun RaiderMarkSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit

@Suppress("unused")
@Composable
internal fun TopChatterBadgeSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit

@Suppress("unused")
@Composable
internal fun PremiumMentionSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit

/** The free build has no notifications page; its title is never shown. */
internal val NotificationSettingsTitle = R.string.settings

/** Customizable notifications are Premium: the row shows locked, and a tap shows the Premium banner. */
@Suppress("unused")
@Composable
internal fun NotificationSettingsEntry(onOpen: () -> Unit) {
    SettingsLockedEntry(
        icon = rememberVectorPainter(Icons.Outlined.Notifications),
        title = stringResource(R.string.settings_section_notifications),
        source = "the notifications settings",
    )
}

@Suppress("unused")
@Composable
internal fun NotificationSettings(
    settings: AppSettings,
    signedIn: Boolean,
    viewModel: SettingsViewModel,
    anchor: SettingsScrollAnchor,
) = Unit
