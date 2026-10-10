package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.staticCompositionLocalOf
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget

/**
 * Opens Settings at a section from inside the stream screen, minimizing the player as the channel's alerts button
 * does; null outside a stream.
 */
internal val LocalOpenSettings = staticCompositionLocalOf<((SettingsScrollTarget) -> Unit)?> { null }
