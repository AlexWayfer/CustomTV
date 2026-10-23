package name.alexwayfer.customtv.ui

import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import name.alexwayfer.customtv.R

/** Android 13 and newer show their own note when an app copies; older versions copy silently. */
internal fun copyNeedsOwnNotice(sdkInt: Int): Boolean = sdkInt < Build.VERSION_CODES.TIRAMISU

/** Copies the system does not confirm, waiting for [CopiedSnackbar] to tell about them. */
internal object CopiedNotices {
    val copies = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Tells about a copy the app just made, where the system does not. */
    fun copied() {
        if (copyNeedsOwnNotice(Build.VERSION.SDK_INT)) copies.tryEmit(Unit)
    }
}

/** Tells in the app's snackbar that the text was copied, on the Android versions that do not tell it themselves. */
@Composable
internal fun CopiedSnackbar(snackbarHostState: SnackbarHostState) {
    val message = stringResource(R.string.copied)
    LaunchedEffect(snackbarHostState, message) {
        var shown: Job? = null
        CopiedNotices.copies.collect {
            // A copy while the note is on screen, or waits for its turn, leaves that note rather than adding another.
            if (shown?.isActive == true) return@collect
            shown = this@LaunchedEffect.launch { snackbarHostState.showSnackbar(message) }
        }
    }
}
