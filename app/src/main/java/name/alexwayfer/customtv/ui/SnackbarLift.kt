package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the app's snackbar goes over the full screen player: lifted [px] off the bottom over its control bar, and
 * [overPlayer], centered on the whole screen and kept between the player's corner buttons.
 */
@Stable
internal class SnackbarLift {
    var px: () -> Int by mutableStateOf({ 0 })
    var overPlayer by mutableStateOf(false)
}
