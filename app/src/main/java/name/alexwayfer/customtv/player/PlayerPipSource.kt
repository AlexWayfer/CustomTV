package name.alexwayfer.customtv.player

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onLayoutRectChanged
import name.alexwayfer.customtv.MainActivity
import name.alexwayfer.customtv.PipVideoSource
import name.alexwayfer.customtv.pipVideoBounds

/** Follows the actual player's window bounds, including mini-player movement and fullscreen letterboxing. */
@Composable
internal fun rememberPlayerPipSource(): Modifier {
    val activity = LocalActivity.current as? MainActivity
    val source = remember { PipVideoSource() }
    DisposableEffect(activity, source) {
        activity?.pipVideoSources?.bind(source)
        onDispose {
            activity?.pipVideoSources?.clear(source)
        }
    }
    return Modifier.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) { coordinates ->
        source.bounds = pipVideoBounds(coordinates.boundsInWindow)
        activity?.pipVideoSources?.update(source)
    }
}
