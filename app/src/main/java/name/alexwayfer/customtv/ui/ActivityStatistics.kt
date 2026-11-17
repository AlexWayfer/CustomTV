package name.alexwayfer.customtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import name.alexwayfer.customtv.data.ActivityStatisticsCollector

/** Background workers and the Premium login screen must not count as app use. */
@Composable
internal fun ActivityStatistics() {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { ActivityStatisticsCollector.collect(context) }
    }
}
