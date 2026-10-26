package name.alexwayfer.customtv.ui.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import name.alexwayfer.customtv.ui.settings.SettingsScrollTarget

/**
 * Opens Settings at a section from inside the stream screen, minimizing the player as the channel's alerts button
 * does; Back from them reopens the sheet given with it. Null outside a stream.
 */
internal val LocalOpenSettings =
    staticCompositionLocalOf<((SettingsScrollTarget, PlayerSheetReturn?) -> Unit)?> { null }

/** One lambda for the screen's life, so the static local never redraws the stream screen. */
@Composable
internal fun rememberOpenSettings(scene: WatchScene): (SettingsScrollTarget, PlayerSheetReturn?) -> Unit {
    val dismissToSettings by rememberUpdatedState<(SettingsScrollTarget, PlayerSheetReturn?) -> Unit>(
        scene::dismissToSettings,
    )
    return remember { { target, sheetReturn -> dismissToSettings(target, sheetReturn) } }
}
