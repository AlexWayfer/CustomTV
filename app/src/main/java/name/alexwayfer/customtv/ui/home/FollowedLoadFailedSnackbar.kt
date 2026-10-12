package name.alexwayfer.customtv.ui.home

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.watch.OneShotRequest

/** Tells that the follow list on screen could not be loaded again, with Retry for another whole load. */
@Composable
internal fun FollowedLoadFailedSnackbar(
    failed: OneShotRequest<Unit>?,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
) {
    val message = stringResource(R.string.channels_load_failed)
    val retry = stringResource(R.string.retry)
    val currentOnRetry by rememberUpdatedState(onRetry)
    LaunchedEffect(failed) {
        failed?.take() ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, actionLabel = retry, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) currentOnRetry()
    }
}
