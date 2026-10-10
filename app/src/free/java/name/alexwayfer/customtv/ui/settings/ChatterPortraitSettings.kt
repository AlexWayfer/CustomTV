package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.data.AppSettings

/** AI chatter portraits are a Premium feature: the free settings have no page for them, so this title is never shown. */
internal val ChatterPortraitSettingsTitle = R.string.settings

@Suppress("unused")
@Composable
internal fun ChatterPortraitSettingsEntry(onOpen: () -> Unit) = Unit

@Suppress("unused")
@Composable
internal fun ChatterPortraitSettings(settings: AppSettings, viewModel: SettingsViewModel) = Unit
