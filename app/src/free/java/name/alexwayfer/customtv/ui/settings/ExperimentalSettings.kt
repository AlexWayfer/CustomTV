package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** The free build has no experimental page; its title is never shown. */
internal val ExperimentalSettingsTitle = R.string.settings

@Suppress("unused")
@Composable
internal fun ExperimentalSettingsEntry(accountName: String?, onOpen: () -> Unit) = Unit

@Suppress("unused")
@Composable
internal fun ExperimentalSettings(accountName: String?, settings: AppSettings, viewModel: SettingsViewModel) = Unit
