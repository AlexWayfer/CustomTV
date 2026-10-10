package name.alexwayfer.customtv.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import name.alexwayfer.customtv.R

/** The free build has no chatter labels page; its title is never shown. */
internal val ChatterLabelsSettingsTitle = R.string.settings

@Suppress("unused")
@Composable
internal fun ChatterLabelsSettingsEntry(onOpen: () -> Unit) = Unit

@Suppress("unused")
@Composable
internal fun ChatterLabelsSettings(seed: ChatterLabelFieldSeed, leaveGuard: SettingsLeaveGuard, modifier: Modifier) = Unit
