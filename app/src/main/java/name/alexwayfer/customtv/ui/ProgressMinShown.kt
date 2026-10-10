package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** How long progress stays once shown, so a load that ends right after it appears does not flash it. */
private val PROGRESS_MIN_SHOWN = 500.milliseconds

/** How much longer progress shown for [shownFor] stays, so it is never on screen for less than the minimum. */
internal fun progressHideDelay(shownFor: Duration): Duration =
    (PROGRESS_MIN_SHOWN - shownFor).coerceAtLeast(Duration.ZERO)

/**
 * The [value] to show, where a value that is [loading] stays on screen for at least the minimum before the next one
 * replaces it.
 */
@Composable
internal fun <T> rememberHeldWhileLoading(value: T, loading: Boolean): T {
    var shown by remember { mutableStateOf(value) }
    var loadingSince by remember { mutableStateOf(if (loading) TimeSource.Monotonic.markNow() else null) }
    LaunchedEffect(value, loading) {
        val since = loadingSince
        if (loading) {
            if (since == null) loadingSince = TimeSource.Monotonic.markNow()
        } else if (since != null) {
            delay(progressHideDelay(since.elapsedNow()))
            loadingSince = null
        }
        shown = value
    }
    return shown
}
