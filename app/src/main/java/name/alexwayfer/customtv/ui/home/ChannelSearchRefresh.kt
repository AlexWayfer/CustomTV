package name.alexwayfer.customtv.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import name.alexwayfer.customtv.data.ChannelSearchHit
import name.alexwayfer.customtv.data.ChannelSearchRepository
import name.alexwayfer.customtv.data.channelSearchQuery
import kotlin.time.Duration.Companion.milliseconds

/** Refreshes only the visible channel list, dropping the previous query as soon as the input changes. */
@Composable
internal fun rememberSearchResults(value: String, viewModel: HomeViewModel): ChannelSearchResults {
    val query = channelSearchQuery(value)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val results = remember(query) { ChannelSearchResults() }
    LaunchedEffect(query, lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            results.hits = emptyList()
            if (query != null) delay(300.milliseconds)
            while (isActive) {
                if (query == null) {
                    viewModel.refreshRecents(showProgress = false, automatic = true)
                } else {
                    results.loading = true
                    try {
                        results.hits = ChannelSearchRepository.suggest(query)
                    } finally {
                        results.loading = false
                    }
                }
                delay(RECENT_AUTO_REFRESH_INTERVAL_MS.milliseconds)
            }
        }
    }
    return results
}

internal class ChannelSearchResults {
    var hits by mutableStateOf(emptyList<ChannelSearchHit>())
    var loading by mutableStateOf(false)
}
