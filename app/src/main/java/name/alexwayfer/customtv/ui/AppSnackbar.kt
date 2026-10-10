package name.alexwayfer.customtv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import name.alexwayfer.customtv.ui.theme.TwitchPurple
import name.alexwayfer.customtv.ui.theme.TwitchSurfaceAlt
import name.alexwayfer.customtv.ui.theme.TwitchText

/**
 * Where the app's snackbar goes over the full screen player: lifted [px] off the bottom over its control bar, and
 * [overPlayer], centered on the whole screen and kept between the player's corner buttons.
 */
@Stable
internal class SnackbarLift {
    var px: () -> Int by mutableStateOf({ 0 })
    var overPlayer by mutableStateOf(false)
}

internal val LocalSnackbarLift = staticCompositionLocalOf<SnackbarLift?> { null }

/** As wide as the note at the top of the full screen player, so it clears the chat button in the corner. */
internal val SnackbarOverPlayerMaxWidth = 480.dp

/** While [active], places the app's snackbar over the full screen player, lifted by [px], read when it is laid out. */
@Composable
internal fun LiftSnackbar(active: Boolean, px: () -> Int) {
    val lift = LocalSnackbarLift.current ?: return
    val currentPx by rememberUpdatedState(px)
    DisposableEffect(lift, active) {
        if (active) {
            lift.px = { currentPx() }
            lift.overPlayer = true
        }
        onDispose {
            lift.px = { 0 }
            lift.overPlayer = false
        }
    }
}

/** The app's snackbar on a dark surface; a gesture tip leads with an info icon and can be swiped away. */
@Composable
internal fun AppSnackbar(data: SnackbarData) {
    if (data.visuals is GestureTipSnackbarVisuals) {
        SwipeAway(onDismiss = data::dismiss) {
            Snackbar(containerColor = TwitchSurfaceAlt, contentColor = TwitchText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TwitchPurple,
                        modifier = Modifier.padding(end = 12.dp).size(20.dp),
                    )
                    Text(data.visuals.message)
                }
            }
        }
    } else {
        Snackbar(
            snackbarData = data,
            containerColor = TwitchSurfaceAlt,
            contentColor = TwitchText,
            actionColor = TwitchPurple,
            dismissActionContentColor = TwitchText,
        )
    }
}
