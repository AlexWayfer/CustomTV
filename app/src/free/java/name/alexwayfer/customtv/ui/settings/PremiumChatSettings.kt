package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
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
internal fun PremiumMentionSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit

/** The free build has no notifications page; its title is never shown. */
internal val NotificationSettingsTitle = R.string.settings

@Suppress("unused")
@Composable
internal fun NotificationSettingsEntry(onOpen: () -> Unit) = Unit

@Suppress("unused")
@Composable
internal fun NotificationSettings(settings: AppSettings, signedIn: Boolean, viewModel: SettingsViewModel) = Unit
